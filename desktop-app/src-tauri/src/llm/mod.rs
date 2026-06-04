use serde_json::json;
use std::fs;
use std::path::PathBuf;
use std::time::Duration;
use windows::core::w;

use crate::db::{
    get_llm_config, insert_llm_review, now_ms, open_connection, save_llm_config,
    today_summary_events, LlmConfig, LlmReview,
};

const KEYRING_SERVICE: &str = "life-debugger-desktop";
const KEYRING_USER: &str = "openai-compatible-api-key";

pub fn default_llm_config() -> LlmConfig {
    LlmConfig {
        provider: "DeepSeek".to_string(),
        base_url: "https://api.deepseek.com".to_string(),
        model: "deepseek-chat".to_string(),
        api_key_stored: false,
    }
}

pub fn load_llm_config() -> Result<LlmConfig, String> {
    let conn = open_connection()?;
    let mut config = get_llm_config(&conn)?.unwrap_or_else(default_llm_config);
    config.api_key_stored = has_saved_api_key();
    Ok(config)
}

pub fn save_config(
    provider: String,
    base_url: String,
    model: String,
    api_key: Option<String>,
) -> Result<LlmConfig, String> {
    let conn = open_connection()?;

    if let Some(api_key) = api_key.filter(|value| !value.trim().is_empty()) {
        save_api_key(api_key.trim())?;
    }

    let existing_key = has_saved_api_key();
    let config = LlmConfig {
        provider,
        base_url: base_url.trim_end_matches('/').to_string(),
        model,
        api_key_stored: existing_key,
    };
    save_llm_config(&conn, &config)?;
    Ok(config)
}

pub fn build_llm_preview() -> Result<String, String> {
    let conn = open_connection()?;
    let events = today_summary_events(&conn)?;
    let allowed_events = events
        .into_iter()
        .filter(|event| event.llm_allowed)
        .map(|event| {
            json!({
                "start_time": event.start_time,
                "end_time": event.end_time,
                "duration_seconds": event.duration_seconds,
                "category": event.category,
                "activity_type": event.activity_type,
                "app_label": event.app_label,
                "window_title": event.window_title,
                "summary": event.summary,
                "privacy_level": event.privacy_level,
            })
        })
        .collect::<Vec<_>>();

    let payload = json!({
        "device": {
            "device_id": "desktop-local",
            "device_type": "desktop",
            "role": "primary_analysis"
        },
        "events": allowed_events,
        "privacy_note": "Only user-previewable SummaryEvent data is included. Raw desktop events, keystrokes, screenshots, microphone, camera and file contents are not collected or sent."
    });

    serde_json::to_string_pretty(&payload)
        .map_err(|error| format!("serialize llm preview failed: {error}"))
}

pub async fn test_connection() -> Result<String, String> {
    let config = load_llm_config()?;
    let api_key = load_api_key()?;
    let content = send_chat_completion(
        &config,
        &api_key,
        vec![json!({
            "role": "user",
            "content": "Reply with exactly: ok"
        })],
        32,
        false,
    )
    .await?;

    Ok(format!("LLM connection ok: {}", content.trim()))
}

pub async fn generate_daily_review() -> Result<LlmReview, String> {
    let config = load_llm_config()?;
    let api_key = load_api_key()?;
    let preview = build_llm_preview()?;
    let system_prompt = "You are Life Debugger. Return only valid JSON. Do not wrap the JSON in markdown. The JSON object must contain keys: daily_summary, time_distribution, attention_shifts, suggestions.";
    let user_prompt = format!(
        "Analyze this user-previewable desktop SummaryEvent package. Do not invent data beyond the package. If there is little data, still return a JSON object explaining that limitation.\n\n{}",
        preview
    );

    let content = send_chat_completion_with_json_fallback(
        &config,
        &api_key,
        vec![
            json!({
                "role": "system",
                "content": system_prompt
            }),
            json!({
                "role": "user",
                "content": user_prompt
            }),
        ],
        1200,
    )
    .await?;

    let parsed: serde_json::Value = serde_json::from_str(&extract_json_object(&content)?)
        .map_err(|error| format!("LLM returned non-JSON content: {error}"))?;
    let content_json = serde_json::to_string_pretty(&parsed)
        .map_err(|error| format!("serialize LLM review JSON failed: {error}"))?;

    let review = LlmReview {
        review_date: review_date(),
        provider: config.provider,
        model: config.model,
        content_json,
        created_at_ms: now_ms(),
    };
    let conn = open_connection()?;
    insert_llm_review(&conn, &review)?;
    Ok(review)
}

pub fn latest_review() -> Result<Option<LlmReview>, String> {
    let conn = open_connection()?;
    crate::db::latest_llm_review(&conn)
}

async fn send_chat_completion(
    config: &LlmConfig,
    api_key: &str,
    messages: Vec<serde_json::Value>,
    max_tokens: u32,
    json_mode: bool,
) -> Result<String, String> {
    let url = format!("{}/chat/completions", config.base_url.trim_end_matches('/'));
    let client = reqwest::Client::builder()
        .connect_timeout(Duration::from_secs(30))
        .timeout(Duration::from_secs(120))
        .build()
        .map_err(|error| format!("build LLM client failed: {error}"))?;
    let mut request_json = json!({
            "model": config.model,
            "messages": messages,
            "temperature": 0.2,
            "max_tokens": max_tokens
    });
    if json_mode {
        request_json["response_format"] = json!({ "type": "json_object" });
    }

    let response = client
        .post(url)
        .bearer_auth(api_key)
        .json(&request_json)
        .send()
        .await
        .map_err(|error| format!("LLM request failed: {error}"))?;

    let status = response.status();
    let body = response
        .text()
        .await
        .map_err(|error| format!("read LLM response failed: {error}"))?;

    if !status.is_success() {
        return Err(format!("LLM request failed with status {status}: {body}"));
    }

    let value: serde_json::Value = serde_json::from_str(&body)
        .map_err(|error| format!("parse LLM response failed: {error}"))?;
    value
        .pointer("/choices/0/message/content")
        .and_then(|content| content.as_str())
        .map(|content| content.to_string())
        .ok_or_else(|| "LLM response missing choices[0].message.content".to_string())
}

async fn send_chat_completion_with_json_fallback(
    config: &LlmConfig,
    api_key: &str,
    messages: Vec<serde_json::Value>,
    max_tokens: u32,
) -> Result<String, String> {
    match send_chat_completion(config, api_key, messages.clone(), max_tokens, true).await {
        Ok(content) => Ok(content),
        Err(json_mode_error) => {
            let fallback = send_chat_completion(config, api_key, messages, max_tokens, false).await;
            fallback.map_err(|fallback_error| {
                format!(
                    "LLM JSON mode failed: {json_mode_error}; fallback failed: {fallback_error}"
                )
            })
        }
    }
}

fn load_api_key() -> Result<String, String> {
    if keyring_enabled() {
        if let Ok(entry) = keyring_entry() {
            if let Ok(api_key) = entry.get_password() {
                return Ok(api_key);
            }
        }
    }

    load_api_key_from_dpapi()
        .map_err(|error| format!("read api key from secure storage failed: {error}"))
}

fn keyring_entry() -> Result<keyring::Entry, String> {
    keyring::Entry::new(KEYRING_SERVICE, KEYRING_USER)
        .map_err(|error| format!("open system credential store failed: {error}"))
}

fn save_api_key(api_key: &str) -> Result<(), String> {
    let mut keyring_verified = false;
    if keyring_enabled() {
        if let Ok(entry) = keyring_entry() {
            if entry.set_password(api_key).is_ok() {
                keyring_verified = entry
                    .get_password()
                    .map(|stored| stored == api_key)
                    .unwrap_or(false);
            }
        }
    }

    // Always keep a DPAPI-encrypted local backup. Some Windows Credential Manager
    // backends can verify immediately but fail to read from a fresh Entry later.
    save_api_key_with_dpapi(api_key)?;

    if keyring_verified || has_saved_api_key() {
        Ok(())
    } else {
        Err("api key secure storage verification failed".to_string())
    }
}

fn has_saved_api_key() -> bool {
    (keyring_enabled()
        && keyring_entry()
            .and_then(|entry| {
                entry
                    .get_password()
                    .map(|value| !value.trim().is_empty())
                    .map_err(|error| error.to_string())
            })
            .unwrap_or(false))
        || load_api_key_from_dpapi()
            .map(|value| !value.trim().is_empty())
            .unwrap_or(false)
}

#[cfg(target_os = "windows")]
fn save_api_key_with_dpapi(api_key: &str) -> Result<(), String> {
    use windows::Win32::Foundation::{LocalFree, HLOCAL};
    use windows::Win32::Security::Cryptography::{
        CryptProtectData, CRYPTPROTECT_UI_FORBIDDEN, CRYPT_INTEGER_BLOB,
    };

    let mut input_bytes = api_key.as_bytes().to_vec();
    let input = CRYPT_INTEGER_BLOB {
        cbData: input_bytes.len() as u32,
        pbData: input_bytes.as_mut_ptr(),
    };
    let mut output = CRYPT_INTEGER_BLOB::default();

    unsafe {
        CryptProtectData(
            &input,
            w!("Life Debugger Desktop API Key"),
            None,
            None,
            None,
            CRYPTPROTECT_UI_FORBIDDEN,
            &mut output,
        )
        .map_err(|error| format!("DPAPI encrypt failed: {error}"))?;

        let encrypted =
            std::slice::from_raw_parts(output.pbData, output.cbData as usize).to_vec();
        let _ = LocalFree(HLOCAL(output.pbData as _));

        let path = dpapi_key_path()?;
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)
                .map_err(|error| format!("create secure key dir failed: {error}"))?;
        }
        fs::write(path, encrypted).map_err(|error| format!("write DPAPI key file failed: {error}"))
    }
}

#[cfg(not(target_os = "windows"))]
fn save_api_key_with_dpapi(_api_key: &str) -> Result<(), String> {
    Err("DPAPI fallback is only available on Windows".to_string())
}

#[cfg(target_os = "windows")]
fn load_api_key_from_dpapi() -> Result<String, String> {
    use windows::Win32::Foundation::{LocalFree, HLOCAL};
    use windows::Win32::Security::Cryptography::{
        CryptUnprotectData, CRYPTPROTECT_UI_FORBIDDEN, CRYPT_INTEGER_BLOB,
    };

    let path = dpapi_key_path()?;
    let mut encrypted =
        fs::read(path).map_err(|error| format!("read DPAPI key file failed: {error}"))?;
    let input = CRYPT_INTEGER_BLOB {
        cbData: encrypted.len() as u32,
        pbData: encrypted.as_mut_ptr(),
    };
    let mut output = CRYPT_INTEGER_BLOB::default();

    unsafe {
        CryptUnprotectData(
            &input,
            None,
            None,
            None,
            None,
            CRYPTPROTECT_UI_FORBIDDEN,
            &mut output,
        )
        .map_err(|error| format!("DPAPI decrypt failed: {error}"))?;

        let plain = std::slice::from_raw_parts(output.pbData, output.cbData as usize).to_vec();
        let _ = LocalFree(HLOCAL(output.pbData as _));
        String::from_utf8(plain).map_err(|error| format!("DPAPI key is not valid UTF-8: {error}"))
    }
}

#[cfg(not(target_os = "windows"))]
fn load_api_key_from_dpapi() -> Result<String, String> {
    Err("DPAPI fallback is only available on Windows".to_string())
}

fn dpapi_key_path() -> Result<PathBuf, String> {
    if let Some(path) = std::env::var_os("LIFE_DEBUGGER_API_KEY_FILE") {
        return Ok(PathBuf::from(path));
    }

    let base = std::env::var_os("LOCALAPPDATA")
        .map(PathBuf::from)
        .or_else(|| std::env::current_dir().ok())
        .ok_or_else(|| "cannot resolve local app data directory".to_string())?;
    Ok(base
        .join("Life Debugger")
        .join("Desktop")
        .join("api_key.dpapi"))
}

fn keyring_enabled() -> bool {
    std::env::var("LIFE_DEBUGGER_DISABLE_KEYRING")
        .map(|value| value != "1")
        .unwrap_or(true)
}

fn review_date() -> String {
    let days = now_ms() / 1000 / 60 / 60 / 24;
    format!("unix-day-{days}")
}

fn extract_json_object(content: &str) -> Result<String, String> {
    let trimmed = content.trim();
    if trimmed.starts_with('{') {
        return Ok(trimmed.to_string());
    }

    let without_fence = trimmed
        .replace("```json", "```")
        .replace("```JSON", "```");
    let candidate = if without_fence.contains("```") {
        without_fence
            .split("```")
            .nth(1)
            .map(str::trim)
            .unwrap_or(trimmed)
            .to_string()
    } else {
        trimmed.to_string()
    };

    if candidate.starts_with('{') {
        return Ok(candidate);
    }

    let start = candidate
        .find('{')
        .ok_or_else(|| "LLM returned no JSON object".to_string())?;
    let end = candidate
        .rfind('}')
        .ok_or_else(|| "LLM returned incomplete JSON object".to_string())?;
    if end <= start {
        return Err("LLM returned invalid JSON object bounds".to_string());
    }

    Ok(candidate[start..=end].to_string())
}
