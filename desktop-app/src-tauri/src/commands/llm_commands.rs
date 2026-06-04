use crate::db::{LlmConfig, LlmReview};
use serde::Deserialize;

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SaveLlmConfigRequest {
    pub provider: String,
    #[serde(alias = "base_url")]
    pub base_url: String,
    pub model: String,
    #[serde(alias = "api_key")]
    pub api_key: Option<String>,
}

#[tauri::command]
pub fn build_llm_preview() -> Result<String, String> {
    crate::llm::build_llm_preview()
}

#[tauri::command]
pub fn get_llm_config() -> Result<LlmConfig, String> {
    crate::llm::load_llm_config()
}

#[tauri::command]
pub fn save_llm_config(request: SaveLlmConfigRequest) -> Result<LlmConfig, String> {
    crate::llm::save_config(
        request.provider,
        request.base_url,
        request.model,
        request.api_key,
    )
}

#[tauri::command]
pub fn save_llm_config_json(request_json: String) -> Result<LlmConfig, String> {
    let request: SaveLlmConfigRequest = serde_json::from_str(&request_json)
        .map_err(|error| format!("parse llm config request failed: {error}"))?;
    crate::llm::save_config(
        request.provider,
        request.base_url,
        request.model,
        request.api_key,
    )
}

#[tauri::command]
pub async fn test_llm_connection() -> Result<String, String> {
    crate::llm::test_connection().await
}

#[tauri::command]
pub async fn generate_daily_review() -> Result<LlmReview, String> {
    crate::llm::generate_daily_review().await
}

#[tauri::command]
pub fn get_latest_review() -> Result<Option<LlmReview>, String> {
    crate::llm::latest_review()
}

#[cfg(test)]
mod tests {
    use super::SaveLlmConfigRequest;

    #[test]
    fn parses_camel_and_snake_case_api_key_fields() {
        let camel: SaveLlmConfigRequest = serde_json::from_str(
            r#"{"provider":"DeepSeek","baseUrl":"https://api.deepseek.com","model":"deepseek-chat","apiKey":"sk-test"}"#,
        )
        .expect("camelCase request should parse");
        assert_eq!(camel.api_key.as_deref(), Some("sk-test"));

        let snake: SaveLlmConfigRequest = serde_json::from_str(
            r#"{"provider":"DeepSeek","base_url":"https://api.deepseek.com","model":"deepseek-chat","api_key":"sk-test"}"#,
        )
        .expect("snake_case request should parse");
        assert_eq!(snake.api_key.as_deref(), Some("sk-test"));
    }

    #[cfg(target_os = "windows")]
    #[test]
    fn save_llm_config_json_persists_api_key_with_dpapi_fallback() {
        let unique = format!(
            "life-debugger-llm-test-{}",
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .expect("system time should be valid")
                .as_nanos()
        );
        let db_path = std::env::temp_dir().join(format!("{unique}.sqlite3"));
        let key_path = std::env::temp_dir().join(format!("{unique}.dpapi"));

        unsafe {
            std::env::set_var("LIFE_DEBUGGER_DISABLE_KEYRING", "1");
            std::env::set_var("LIFE_DEBUGGER_DB_FILE", &db_path);
            std::env::set_var("LIFE_DEBUGGER_API_KEY_FILE", &key_path);
        }

        let config = super::save_llm_config_json(
            r#"{"provider":"DeepSeek","baseUrl":"https://api.deepseek.com","model":"deepseek-chat","apiKey":"sk-test-local-only"}"#.to_string(),
        )
        .expect("config save should succeed");
        assert!(config.api_key_stored);

        let loaded = crate::llm::load_llm_config().expect("config should load");
        assert!(loaded.api_key_stored);

        let _ = std::fs::remove_file(db_path);
        let _ = std::fs::remove_file(key_path);
        unsafe {
            std::env::remove_var("LIFE_DEBUGGER_DISABLE_KEYRING");
            std::env::remove_var("LIFE_DEBUGGER_DB_FILE");
            std::env::remove_var("LIFE_DEBUGGER_API_KEY_FILE");
        }
    }
}
