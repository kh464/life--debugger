use rusqlite::{params, Connection, OptionalExtension};
use base64::Engine;
use rand::RngCore;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};
use uuid::Uuid;

use crate::collector::active_window::ActiveWindowInfo;

const DEVICE_ID: &str = "desktop-local";
const DB_FILE_NAME: &str = "life-debugger.sqlite3";

#[derive(Debug, Clone)]
pub struct RawDesktopEvent {
    pub collected_at_ms: i64,
    pub app_label: String,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
    pub idle_seconds: i64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SummaryEvent {
    pub event_id: String,
    pub device_id: String,
    pub device_type: String,
    pub source: String,
    pub start_time: i64,
    pub end_time: Option<i64>,
    pub duration_seconds: Option<i64>,
    pub category: Option<String>,
    pub activity_type: Option<String>,
    pub app_label: Option<String>,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
    pub summary: String,
    pub confidence: f64,
    pub privacy_level: String,
    pub llm_allowed: bool,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DashboardState {
    pub total_desktop_minutes: i64,
    pub productive_minutes: i64,
    pub summary_event_count: i64,
    pub app_switch_count: i64,
    pub top_categories: String,
    pub llm_configured: bool,
    pub collector_running: bool,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmConfig {
    pub provider: String,
    pub base_url: String,
    pub model: String,
    pub api_key_stored: bool,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmReview {
    pub review_date: String,
    pub provider: String,
    pub model: String,
    pub content_json: String,
    pub created_at_ms: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DeviceIdentity {
    pub device_id: String,
    pub device_name: String,
    pub device_type: String,
    pub public_key: String,
    pub private_key_ref: Option<String>,
    pub created_at: i64,
    pub updated_at: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PairedDevice {
    pub paired_device_id: String,
    pub paired_device_name: String,
    pub paired_device_type: String,
    pub public_key: String,
    pub last_known_host: Option<String>,
    pub last_known_port: Option<i64>,
    pub pairing_status: String,
    pub trust_level: String,
    pub paired_at: i64,
    pub last_seen_at: Option<i64>,
    pub last_sync_at: Option<i64>,
    pub metadata_json: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PairingSession {
    pub session_id: String,
    pub pairing_token: String,
    pub local_device_id: String,
    pub local_device_type: String,
    pub local_host: Option<String>,
    pub local_port: Option<i64>,
    pub expires_at: i64,
    pub status: String,
    pub created_at: i64,
}

pub fn now_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as i64
}

pub fn open_connection() -> Result<Connection, String> {
    let path = database_path()?;
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent).map_err(|error| format!("create db dir failed: {error}"))?;
    }

    let conn = Connection::open(path).map_err(|error| format!("open sqlite failed: {error}"))?;
    init_schema(&conn)?;
    Ok(conn)
}

pub fn database_path() -> Result<PathBuf, String> {
    if let Some(path) = std::env::var_os("LIFE_DEBUGGER_DB_FILE") {
        return Ok(PathBuf::from(path));
    }

    let base = std::env::var_os("LOCALAPPDATA")
        .map(PathBuf::from)
        .or_else(|| std::env::current_dir().ok())
        .ok_or_else(|| "cannot resolve local app data directory".to_string())?;

    Ok(base
        .join("Life Debugger")
        .join("Desktop")
        .join(DB_FILE_NAME))
}

fn init_schema(conn: &Connection) -> Result<(), String> {
    conn.execute_batch(
        r#"
        CREATE TABLE IF NOT EXISTS raw_desktop_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            collected_at_ms INTEGER NOT NULL,
            app_label TEXT NOT NULL,
            window_title TEXT,
            process_name TEXT,
            idle_seconds INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS summary_events (
            event_id TEXT PRIMARY KEY,
            device_id TEXT NOT NULL,
            device_type TEXT NOT NULL,
            source TEXT NOT NULL,
            start_time_ms INTEGER NOT NULL,
            end_time_ms INTEGER,
            duration_seconds INTEGER,
            category TEXT,
            activity_type TEXT,
            app_label TEXT,
            window_title TEXT,
            process_name TEXT,
            summary TEXT NOT NULL,
            confidence REAL NOT NULL,
            privacy_level TEXT NOT NULL,
            llm_allowed INTEGER NOT NULL,
            sync_status TEXT NOT NULL DEFAULT 'local_only',
            created_at_ms INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS llm_configs (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            provider TEXT NOT NULL,
            base_url TEXT NOT NULL,
            model TEXT NOT NULL,
            api_key_stored INTEGER NOT NULL DEFAULT 0,
            updated_at_ms INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS llm_reviews (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            review_date TEXT NOT NULL,
            provider TEXT NOT NULL,
            model TEXT NOT NULL,
            content_json TEXT NOT NULL,
            created_at_ms INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS manual_intent_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            created_at_ms INTEGER NOT NULL,
            intent_label TEXT NOT NULL,
            note TEXT
        );

        CREATE TABLE IF NOT EXISTS privacy_settings (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            allow_window_title_to_llm INTEGER NOT NULL DEFAULT 1,
            allow_process_name_to_llm INTEGER NOT NULL DEFAULT 0,
            updated_at_ms INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS device_identity (
            device_id TEXT PRIMARY KEY,
            device_name TEXT NOT NULL,
            device_type TEXT NOT NULL,
            public_key TEXT NOT NULL,
            private_key_ref TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS paired_devices (
            paired_device_id TEXT PRIMARY KEY,
            paired_device_name TEXT NOT NULL,
            paired_device_type TEXT NOT NULL,
            public_key TEXT NOT NULL,
            last_known_host TEXT,
            last_known_port INTEGER,
            pairing_status TEXT NOT NULL,
            trust_level TEXT DEFAULT 'trusted',
            paired_at INTEGER NOT NULL,
            last_seen_at INTEGER,
            last_sync_at INTEGER,
            metadata_json TEXT
        );

        CREATE TABLE IF NOT EXISTS pairing_sessions (
            session_id TEXT PRIMARY KEY,
            pairing_token TEXT NOT NULL,
            local_device_id TEXT NOT NULL,
            local_device_type TEXT NOT NULL,
            local_host TEXT,
            local_port INTEGER,
            expires_at INTEGER NOT NULL,
            status TEXT NOT NULL,
            created_at INTEGER NOT NULL
        );
        "#,
    )
    .map_err(|error| format!("init sqlite schema failed: {error}"))?;

    Ok(())
}

pub fn insert_raw_event(
    conn: &Connection,
    active_window: &ActiveWindowInfo,
    idle_seconds: i64,
) -> Result<RawDesktopEvent, String> {
    let event = RawDesktopEvent {
        collected_at_ms: now_ms(),
        app_label: active_window.app_name.clone(),
        window_title: active_window.window_title.clone(),
        process_name: active_window.process_name.clone(),
        idle_seconds,
    };

    conn.execute(
        r#"
        INSERT INTO raw_desktop_events (
            collected_at_ms, app_label, window_title, process_name, idle_seconds
        ) VALUES (?1, ?2, ?3, ?4, ?5)
        "#,
        params![
            event.collected_at_ms,
            event.app_label,
            event.window_title,
            event.process_name,
            event.idle_seconds
        ],
    )
    .map_err(|error| format!("insert raw desktop event failed: {error}"))?;

    Ok(event)
}

pub fn last_raw_event(conn: &Connection) -> Result<Option<RawDesktopEvent>, String> {
    conn.query_row(
        r#"
        SELECT collected_at_ms, app_label, window_title, process_name, idle_seconds
        FROM raw_desktop_events
        ORDER BY collected_at_ms DESC
        LIMIT 1
        "#,
        [],
        |row| {
            Ok(RawDesktopEvent {
                collected_at_ms: row.get(0)?,
                app_label: row.get(1)?,
                window_title: row.get(2)?,
                process_name: row.get(3)?,
                idle_seconds: row.get(4)?,
            })
        },
    )
    .optional()
    .map_err(|error| format!("query last raw event failed: {error}"))
}

pub fn insert_summary_event(conn: &Connection, event: &SummaryEvent) -> Result<(), String> {
    conn.execute(
        r#"
        INSERT OR REPLACE INTO summary_events (
            event_id, device_id, device_type, source, start_time_ms, end_time_ms,
            duration_seconds, category, activity_type, app_label, window_title,
            process_name, summary, confidence, privacy_level, llm_allowed,
            sync_status, created_at_ms
        ) VALUES (
            ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, 'local_only', ?17
        )
        "#,
        params![
            event.event_id,
            event.device_id,
            event.device_type,
            event.source,
            event.start_time,
            event.end_time,
            event.duration_seconds,
            event.category,
            event.activity_type,
            event.app_label,
            event.window_title,
            event.process_name,
            event.summary,
            event.confidence,
            event.privacy_level,
            i64::from(event.llm_allowed),
            now_ms()
        ],
    )
    .map_err(|error| format!("insert summary event failed: {error}"))?;

    Ok(())
}

pub fn today_summary_events(conn: &Connection) -> Result<Vec<SummaryEvent>, String> {
    let since = now_ms() - 24 * 60 * 60 * 1000;
    let mut stmt = conn
        .prepare(
            r#"
            SELECT event_id, device_id, device_type, source, start_time_ms, end_time_ms,
                   duration_seconds, category, activity_type, app_label, window_title,
                   process_name, summary, confidence, privacy_level, llm_allowed
            FROM summary_events
            WHERE start_time_ms >= ?1
            ORDER BY start_time_ms DESC
            "#,
        )
        .map_err(|error| format!("prepare summary query failed: {error}"))?;

    let rows = stmt
        .query_map([since], |row| {
            Ok(SummaryEvent {
                event_id: row.get(0)?,
                device_id: row.get(1)?,
                device_type: row.get(2)?,
                source: row.get(3)?,
                start_time: row.get(4)?,
                end_time: row.get(5)?,
                duration_seconds: row.get(6)?,
                category: row.get(7)?,
                activity_type: row.get(8)?,
                app_label: row.get(9)?,
                window_title: row.get(10)?,
                process_name: row.get(11)?,
                summary: row.get(12)?,
                confidence: row.get(13)?,
                privacy_level: row.get(14)?,
                llm_allowed: row.get::<_, i64>(15)? != 0,
            })
        })
        .map_err(|error| format!("query summary events failed: {error}"))?;

    rows.collect::<Result<Vec<_>, _>>()
        .map_err(|error| format!("map summary events failed: {error}"))
}

pub fn dashboard_state(
    conn: &Connection,
    collector_running: bool,
) -> Result<DashboardState, String> {
    let events = today_summary_events(conn)?;
    let total_seconds: i64 = events
        .iter()
        .map(|event| event.duration_seconds.unwrap_or_default())
        .sum();
    let productive_seconds: i64 = events
        .iter()
        .filter(|event| event.activity_type.as_deref() == Some("productive"))
        .map(|event| event.duration_seconds.unwrap_or_default())
        .sum();
    let categories = events
        .iter()
        .filter_map(|event| event.category.as_deref())
        .fold(Vec::<String>::new(), |mut acc, category| {
            if !acc.iter().any(|item| item == category) {
                acc.push(category.to_string());
            }
            acc
        });

    Ok(DashboardState {
        total_desktop_minutes: total_seconds / 60,
        productive_minutes: productive_seconds / 60,
        summary_event_count: events.len() as i64,
        app_switch_count: events.len().saturating_sub(1) as i64,
        top_categories: if categories.is_empty() {
            "暂无".to_string()
        } else {
            categories.join(", ")
        },
        llm_configured: llm_configured(conn)?,
        collector_running,
    })
}

pub fn llm_configured(conn: &Connection) -> Result<bool, String> {
    conn.query_row(
        "SELECT api_key_stored FROM llm_configs WHERE id = 1",
        [],
        |row| row.get::<_, i64>(0),
    )
    .optional()
    .map(|value| value.unwrap_or_default() != 0)
    .map_err(|error| format!("query llm config failed: {error}"))
}

pub fn save_llm_config(conn: &Connection, config: &LlmConfig) -> Result<(), String> {
    conn.execute(
        r#"
        INSERT INTO llm_configs (id, provider, base_url, model, api_key_stored, updated_at_ms)
        VALUES (1, ?1, ?2, ?3, ?4, ?5)
        ON CONFLICT(id) DO UPDATE SET
            provider = excluded.provider,
            base_url = excluded.base_url,
            model = excluded.model,
            api_key_stored = excluded.api_key_stored,
            updated_at_ms = excluded.updated_at_ms
        "#,
        params![
            config.provider,
            config.base_url,
            config.model,
            if config.api_key_stored { 1 } else { 0 },
            now_ms()
        ],
    )
    .map_err(|error| format!("save llm config failed: {error}"))?;

    Ok(())
}

pub fn get_llm_config(conn: &Connection) -> Result<Option<LlmConfig>, String> {
    conn.query_row(
        r#"
        SELECT provider, base_url, model, api_key_stored
        FROM llm_configs
        WHERE id = 1
        "#,
        [],
        |row| {
            Ok(LlmConfig {
                provider: row.get(0)?,
                base_url: row.get(1)?,
                model: row.get(2)?,
                api_key_stored: row.get::<_, i64>(3)? != 0,
            })
        },
    )
    .optional()
    .map_err(|error| format!("get llm config failed: {error}"))
}

pub fn insert_llm_review(conn: &Connection, review: &LlmReview) -> Result<(), String> {
    conn.execute(
        r#"
        INSERT INTO llm_reviews (review_date, provider, model, content_json, created_at_ms)
        VALUES (?1, ?2, ?3, ?4, ?5)
        "#,
        params![
            review.review_date,
            review.provider,
            review.model,
            review.content_json,
            review.created_at_ms
        ],
    )
    .map_err(|error| format!("insert llm review failed: {error}"))?;

    Ok(())
}

pub fn insert_manual_intent(conn: &Connection, intent_label: &str) -> Result<SummaryEvent, String> {
    let timestamp = now_ms();
    conn.execute(
        r#"
        INSERT INTO manual_intent_events (created_at_ms, intent_label, note)
        VALUES (?1, ?2, NULL)
        "#,
        params![timestamp, intent_label],
    )
    .map_err(|error| format!("insert manual intent failed: {error}"))?;

    let event = SummaryEvent {
        event_id: format!("intent-{timestamp}"),
        device_id: device_id().to_string(),
        device_type: "desktop".to_string(),
        source: "manual_intent".to_string(),
        start_time: timestamp,
        end_time: Some(timestamp),
        duration_seconds: Some(0),
        category: Some(intent_label.to_string()),
        activity_type: Some("intent".to_string()),
        app_label: Some("Manual Intent".to_string()),
        window_title: None,
        process_name: None,
        summary: format!("用户记录了当前意图：{}。", intent_label),
        confidence: 1.0,
        privacy_level: "user_input".to_string(),
        llm_allowed: true,
    };
    insert_summary_event(conn, &event)?;
    Ok(event)
}

pub fn latest_llm_review(conn: &Connection) -> Result<Option<LlmReview>, String> {
    conn.query_row(
        r#"
        SELECT review_date, provider, model, content_json, created_at_ms
        FROM llm_reviews
        ORDER BY created_at_ms DESC
        LIMIT 1
        "#,
        [],
        |row| {
            Ok(LlmReview {
                review_date: row.get(0)?,
                provider: row.get(1)?,
                model: row.get(2)?,
                content_json: row.get(3)?,
                created_at_ms: row.get(4)?,
            })
        },
    )
    .optional()
    .map_err(|error| format!("query latest llm review failed: {error}"))
}

pub fn clear_raw_desktop_events(conn: &Connection) -> Result<usize, String> {
    conn.execute("DELETE FROM raw_desktop_events", [])
        .map_err(|error| format!("clear raw desktop events failed: {error}"))
}

pub fn clear_summary_events(conn: &Connection) -> Result<usize, String> {
    conn.execute("DELETE FROM summary_events", [])
        .map_err(|error| format!("clear summary events failed: {error}"))
}

pub fn export_today_summary_json(conn: &Connection) -> Result<String, String> {
    let events = today_summary_events(conn)?;
    serde_json::to_string_pretty(&events)
        .map_err(|error| format!("export summary events failed: {error}"))
}

pub fn device_id() -> &'static str {
    DEVICE_ID
}

pub fn get_device_identity(conn: &Connection) -> Result<Option<DeviceIdentity>, String> {
    conn.query_row(
        r#"
        SELECT device_id, device_name, device_type, public_key, private_key_ref, created_at, updated_at
        FROM device_identity
        LIMIT 1
        "#,
        [],
        map_device_identity,
    )
    .optional()
    .map_err(|error| format!("get device identity failed: {error}"))
}

pub fn get_or_create_device_identity(conn: &Connection) -> Result<DeviceIdentity, String> {
    if let Some(identity) = get_device_identity(conn)? {
        return Ok(identity);
    }

    let timestamp = now_ms();
    let identity = DeviceIdentity {
        device_id: Uuid::new_v4().to_string(),
        device_name: default_device_name(),
        device_type: "desktop".to_string(),
        public_key: random_base64(32),
        private_key_ref: None,
        created_at: timestamp,
        updated_at: timestamp,
    };

    conn.execute(
        r#"
        INSERT INTO device_identity (
            device_id, device_name, device_type, public_key, private_key_ref, created_at, updated_at
        ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
        "#,
        params![
            identity.device_id,
            identity.device_name,
            identity.device_type,
            identity.public_key,
            identity.private_key_ref,
            identity.created_at,
            identity.updated_at
        ],
    )
    .map_err(|error| format!("insert device identity failed: {error}"))?;

    Ok(identity)
}

pub fn update_device_name(conn: &Connection, device_name: &str) -> Result<DeviceIdentity, String> {
    let identity = get_or_create_device_identity(conn)?;
    let cleaned = device_name.trim();
    if cleaned.is_empty() {
        return Err("device name cannot be empty".to_string());
    }

    conn.execute(
        "UPDATE device_identity SET device_name = ?1, updated_at = ?2 WHERE device_id = ?3",
        params![cleaned, now_ms(), identity.device_id],
    )
    .map_err(|error| format!("update device name failed: {error}"))?;

    get_or_create_device_identity(conn)
}

pub fn reset_device_identity(conn: &Connection) -> Result<(), String> {
    conn.execute("DELETE FROM device_identity", [])
        .map_err(|error| format!("reset device identity failed: {error}"))?;
    Ok(())
}

pub fn upsert_paired_device(conn: &Connection, device: &PairedDevice) -> Result<(), String> {
    conn.execute(
        r#"
        INSERT INTO paired_devices (
            paired_device_id, paired_device_name, paired_device_type, public_key,
            last_known_host, last_known_port, pairing_status, trust_level, paired_at,
            last_seen_at, last_sync_at, metadata_json
        ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)
        ON CONFLICT(paired_device_id) DO UPDATE SET
            paired_device_name = excluded.paired_device_name,
            paired_device_type = excluded.paired_device_type,
            public_key = excluded.public_key,
            last_known_host = excluded.last_known_host,
            last_known_port = excluded.last_known_port,
            pairing_status = excluded.pairing_status,
            trust_level = excluded.trust_level,
            paired_at = excluded.paired_at,
            last_seen_at = excluded.last_seen_at,
            last_sync_at = excluded.last_sync_at,
            metadata_json = excluded.metadata_json
        "#,
        params![
            device.paired_device_id,
            device.paired_device_name,
            device.paired_device_type,
            device.public_key,
            device.last_known_host,
            device.last_known_port,
            device.pairing_status,
            device.trust_level,
            device.paired_at,
            device.last_seen_at,
            device.last_sync_at,
            device.metadata_json
        ],
    )
    .map_err(|error| format!("upsert paired device failed: {error}"))?;
    Ok(())
}

pub fn get_paired_devices(conn: &Connection) -> Result<Vec<PairedDevice>, String> {
    let mut stmt = conn
        .prepare(
            r#"
            SELECT paired_device_id, paired_device_name, paired_device_type, public_key,
                   last_known_host, last_known_port, pairing_status, trust_level, paired_at,
                   last_seen_at, last_sync_at, metadata_json
            FROM paired_devices
            ORDER BY paired_at DESC
            "#,
        )
        .map_err(|error| format!("prepare paired device query failed: {error}"))?;

    let rows = stmt
        .query_map([], |row| {
            Ok(PairedDevice {
                paired_device_id: row.get(0)?,
                paired_device_name: row.get(1)?,
                paired_device_type: row.get(2)?,
                public_key: row.get(3)?,
                last_known_host: row.get(4)?,
                last_known_port: row.get(5)?,
                pairing_status: row.get(6)?,
                trust_level: row.get(7)?,
                paired_at: row.get(8)?,
                last_seen_at: row.get(9)?,
                last_sync_at: row.get(10)?,
                metadata_json: row.get(11)?,
            })
        })
        .map_err(|error| format!("query paired devices failed: {error}"))?;

    rows.collect::<Result<Vec<_>, _>>()
        .map_err(|error| format!("map paired devices failed: {error}"))
}

pub fn remove_paired_device(conn: &Connection, device_id: &str) -> Result<usize, String> {
    conn.execute(
        "DELETE FROM paired_devices WHERE paired_device_id = ?1",
        params![device_id],
    )
    .map_err(|error| format!("remove paired device failed: {error}"))
}

pub fn paired_device_exists(conn: &Connection, device_id: &str) -> Result<bool, String> {
    conn.query_row(
        "SELECT 1 FROM paired_devices WHERE paired_device_id = ?1 LIMIT 1",
        params![device_id],
        |_| Ok(()),
    )
    .optional()
    .map(|value| value.is_some())
    .map_err(|error| format!("query paired device existence failed: {error}"))
}

pub fn insert_pairing_session(conn: &Connection, session: &PairingSession) -> Result<(), String> {
    conn.execute(
        r#"
        INSERT INTO pairing_sessions (
            session_id, pairing_token, local_device_id, local_device_type,
            local_host, local_port, expires_at, status, created_at
        ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)
        "#,
        params![
            session.session_id,
            session.pairing_token,
            session.local_device_id,
            session.local_device_type,
            session.local_host,
            session.local_port,
            session.expires_at,
            session.status,
            session.created_at
        ],
    )
    .map_err(|error| format!("insert pairing session failed: {error}"))?;
    Ok(())
}

pub fn get_pairing_session(conn: &Connection, session_id: &str) -> Result<Option<PairingSession>, String> {
    conn.query_row(
        r#"
        SELECT session_id, pairing_token, local_device_id, local_device_type,
               local_host, local_port, expires_at, status, created_at
        FROM pairing_sessions
        WHERE session_id = ?1
        "#,
        params![session_id],
        |row| {
            Ok(PairingSession {
                session_id: row.get(0)?,
                pairing_token: row.get(1)?,
                local_device_id: row.get(2)?,
                local_device_type: row.get(3)?,
                local_host: row.get(4)?,
                local_port: row.get(5)?,
                expires_at: row.get(6)?,
                status: row.get(7)?,
                created_at: row.get(8)?,
            })
        },
    )
    .optional()
    .map_err(|error| format!("get pairing session failed: {error}"))
}

pub fn update_pairing_session_status(
    conn: &Connection,
    session_id: &str,
    status: &str,
) -> Result<(), String> {
    conn.execute(
        "UPDATE pairing_sessions SET status = ?1 WHERE session_id = ?2",
        params![status, session_id],
    )
    .map_err(|error| format!("update pairing session status failed: {error}"))?;
    Ok(())
}

pub fn expire_old_pairing_sessions(conn: &Connection) -> Result<(), String> {
    conn.execute(
        "UPDATE pairing_sessions SET status = 'expired' WHERE status = 'waiting' AND expires_at <= ?1",
        params![now_ms()],
    )
    .map_err(|error| format!("expire pairing sessions failed: {error}"))?;
    Ok(())
}

pub fn random_base64(byte_len: usize) -> String {
    let mut bytes = vec![0_u8; byte_len];
    rand::rngs::OsRng.fill_bytes(&mut bytes);
    base64::engine::general_purpose::STANDARD_NO_PAD.encode(bytes)
}

fn default_device_name() -> String {
    std::env::var("COMPUTERNAME")
        .or_else(|_| std::env::var("HOSTNAME"))
        .unwrap_or_else(|_| "Life Debugger Desktop".to_string())
}

fn map_device_identity(row: &rusqlite::Row<'_>) -> rusqlite::Result<DeviceIdentity> {
    Ok(DeviceIdentity {
        device_id: row.get(0)?,
        device_name: row.get(1)?,
        device_type: row.get(2)?,
        public_key: row.get(3)?,
        private_key_ref: row.get(4)?,
        created_at: row.get(5)?,
        updated_at: row.get(6)?,
    })
}
