use crate::collector::active_window::{get_active_window, ActiveWindowInfo};
use crate::db::{
    dashboard_state, insert_raw_event, insert_summary_event, last_raw_event, open_connection,
    DashboardState,
};
use crate::summarize::build_summary_event;
use serde::Serialize;
use std::sync::{Mutex, OnceLock};
use std::thread;
use std::time::Duration;

static COLLECTOR_RUNNING: OnceLock<Mutex<bool>> = OnceLock::new();

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CollectorStatus {
    pub running: bool,
    pub current_app: String,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
    pub idle_seconds: i64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RefreshResult {
    pub generated: i64,
}

#[tauri::command]
pub fn get_current_active_window() -> Result<ActiveWindowInfo, String> {
    get_active_window()
}

#[tauri::command]
pub fn get_idle_seconds() -> Result<i64, String> {
    crate::collector::idle_detector::get_idle_seconds()
}

#[tauri::command]
pub fn get_collector_status() -> Result<CollectorStatus, String> {
    let active_window = get_active_window()?;
    let idle_seconds = crate::collector::idle_detector::get_idle_seconds()?;

    Ok(CollectorStatus {
        running: collector_running(),
        current_app: active_window.app_name,
        window_title: active_window.window_title,
        process_name: active_window.process_name,
        idle_seconds,
    })
}

#[tauri::command]
pub fn set_collector_running(running: bool) -> Result<(), String> {
    {
        let mut guard = collector_state()
            .lock()
            .map_err(|_| "collector state lock poisoned".to_string())?;
        *guard = running;
    }
    if running {
        let _ = collect_current_snapshot();
    }
    Ok(())
}

#[tauri::command]
pub fn refresh_today() -> Result<RefreshResult, String> {
    collect_current_snapshot()?;

    Ok(RefreshResult { generated: 1 })
}

#[tauri::command]
pub fn get_dashboard() -> Result<DashboardState, String> {
    let conn = open_connection()?;
    dashboard_state(&conn, collector_running())
}

fn collector_running() -> bool {
    collector_state()
        .lock()
        .map(|guard| *guard)
        .unwrap_or(false)
}

fn collector_state() -> &'static Mutex<bool> {
    COLLECTOR_RUNNING.get_or_init(|| Mutex::new(false))
}

pub fn spawn_background_collector() {
    thread::spawn(|| loop {
        thread::sleep(Duration::from_secs(10));
        if collector_running() {
            let _ = collect_current_snapshot();
        }
    });
}

fn collect_current_snapshot() -> Result<(), String> {
    let conn = open_connection()?;
    let previous = last_raw_event(&conn)?;
    let active_window = get_active_window()?;
    let idle_seconds = crate::collector::idle_detector::get_idle_seconds()?;
    let raw_event = insert_raw_event(&conn, &active_window, idle_seconds)?;
    let summary = build_summary_event(&raw_event, previous.as_ref());
    insert_summary_event(&conn, &summary)
}
