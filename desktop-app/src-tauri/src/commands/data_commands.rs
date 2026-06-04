use serde::Serialize;

use crate::db::{
    clear_raw_desktop_events, clear_summary_events, export_today_summary_json, open_connection,
};

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ClearResult {
    pub deleted: usize,
}

#[tauri::command]
pub fn clear_raw_events() -> Result<ClearResult, String> {
    let conn = open_connection()?;
    Ok(ClearResult {
        deleted: clear_raw_desktop_events(&conn)?,
    })
}

#[tauri::command]
pub fn clear_summary_events_command() -> Result<ClearResult, String> {
    let conn = open_connection()?;
    Ok(ClearResult {
        deleted: clear_summary_events(&conn)?,
    })
}

#[tauri::command]
pub fn export_today_summary_events_json() -> Result<String, String> {
    let conn = open_connection()?;
    export_today_summary_json(&conn)
}
