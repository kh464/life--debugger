use crate::db::{open_connection, today_summary_events, SummaryEvent};

#[tauri::command]
pub fn get_today_summary_events() -> Result<Vec<SummaryEvent>, String> {
    let conn = open_connection()?;
    today_summary_events(&conn)
}
