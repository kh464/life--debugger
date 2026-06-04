use crate::db::{insert_manual_intent, open_connection, SummaryEvent};

#[tauri::command]
pub fn record_manual_intent(intent_label: String) -> Result<SummaryEvent, String> {
    let conn = open_connection()?;
    insert_manual_intent(&conn, &intent_label)
}
