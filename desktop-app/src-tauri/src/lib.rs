mod collector;
mod commands;
mod db;
mod llm;
mod privacy;
mod summarize;
mod sync;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|_| {
            commands::collector_commands::spawn_background_collector();
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            commands::collector_commands::get_current_active_window,
            commands::collector_commands::get_idle_seconds,
            commands::collector_commands::get_collector_status,
            commands::collector_commands::set_collector_running,
            commands::collector_commands::refresh_today,
            commands::collector_commands::get_dashboard,
            commands::timeline_commands::get_today_summary_events,
            commands::llm_commands::build_llm_preview,
            commands::llm_commands::get_llm_config,
            commands::llm_commands::save_llm_config,
            commands::llm_commands::save_llm_config_json,
            commands::llm_commands::test_llm_connection,
            commands::llm_commands::generate_daily_review,
            commands::llm_commands::get_latest_review,
            commands::intent_commands::record_manual_intent,
            commands::data_commands::clear_raw_events,
            commands::data_commands::clear_summary_events_command,
            commands::data_commands::export_today_summary_events_json,
            commands::device_commands::get_local_device_identity,
            commands::device_commands::update_local_device_name,
            commands::device_commands::reset_local_device_identity,
            commands::device_commands::get_paired_devices,
            commands::device_commands::remove_paired_device,
            commands::pairing_commands::start_pairing_session,
            commands::pairing_commands::cancel_pairing_session,
            commands::pairing_commands::get_pairing_session_status,
        ])
        .run(tauri::generate_context!())
        .expect("error while running Life Debugger Desktop");
}
