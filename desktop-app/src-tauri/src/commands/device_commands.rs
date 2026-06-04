use crate::db;

#[tauri::command]
pub async fn get_local_device_identity() -> Result<db::DeviceIdentity, String> {
    let conn = db::open_connection()?;
    db::get_or_create_device_identity(&conn)
}

#[tauri::command]
pub async fn update_local_device_name(device_name: String) -> Result<db::DeviceIdentity, String> {
    let conn = db::open_connection()?;
    db::update_device_name(&conn, &device_name)
}

#[tauri::command]
pub async fn reset_local_device_identity() -> Result<(), String> {
    let conn = db::open_connection()?;
    db::reset_device_identity(&conn)
}

#[tauri::command]
pub async fn get_paired_devices() -> Result<Vec<db::PairedDevice>, String> {
    let conn = db::open_connection()?;
    db::get_paired_devices(&conn)
}

#[tauri::command]
pub async fn remove_paired_device(device_id: String) -> Result<(), String> {
    let conn = db::open_connection()?;
    db::remove_paired_device(&conn, &device_id)?;
    Ok(())
}
