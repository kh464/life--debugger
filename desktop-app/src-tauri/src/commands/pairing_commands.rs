use crate::sync;

#[tauri::command]
pub async fn start_pairing_session() -> Result<sync::PairingQrPayload, String> {
    sync::start_pairing_session()
}

#[tauri::command]
pub async fn cancel_pairing_session(session_id: String) -> Result<(), String> {
    sync::cancel_pairing_session(&session_id)
}

#[tauri::command]
pub async fn get_pairing_session_status(
    session_id: String,
) -> Result<sync::PairingSessionStatus, String> {
    sync::pairing_session_status(&session_id)
}
