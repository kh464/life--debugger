use crate::db::{device_id, now_ms, RawDesktopEvent, SummaryEvent};

pub fn build_summary_event(
    current: &RawDesktopEvent,
    previous: Option<&RawDesktopEvent>,
) -> SummaryEvent {
    let end_time = current.collected_at_ms;
    let start_time = previous
        .map(|event| event.collected_at_ms)
        .filter(|time| *time < end_time)
        .unwrap_or_else(|| end_time.saturating_sub(60_000));

    let duration_seconds = ((end_time - start_time) / 1000).clamp(30, 30 * 60);
    let category = classify_category(&current.app_label, current.process_name.as_deref());
    let activity_type = classify_activity(&category, current.idle_seconds);

    SummaryEvent {
        event_id: format!("desktop-{}", now_ms()),
        device_id: device_id().to_string(),
        device_type: "desktop".to_string(),
        source: "desktop_window_tracker".to_string(),
        start_time,
        end_time: Some(end_time),
        duration_seconds: Some(duration_seconds),
        category: Some(category.clone()),
        activity_type: Some(activity_type.to_string()),
        app_label: Some(current.app_label.clone()),
        window_title: current.window_title.clone(),
        process_name: current.process_name.clone(),
        summary: build_summary_text(current, &category, duration_seconds),
        confidence: 0.78,
        privacy_level: "metadata".to_string(),
        llm_allowed: true,
    }
}

fn build_summary_text(event: &RawDesktopEvent, category: &str, duration_seconds: i64) -> String {
    let minutes = (duration_seconds.max(60) + 59) / 60;
    let title_hint = event
        .window_title
        .as_deref()
        .filter(|title| !title.trim().is_empty())
        .map(|title| format!("，窗口标题为「{}」", title))
        .unwrap_or_default();

    format!(
        "桌面端在 {} 中进行了约 {} 分钟的 {} 活动{}。",
        event.app_label, minutes, category, title_hint
    )
}

fn classify_category(app_label: &str, process_name: Option<&str>) -> String {
    let value = format!(
        "{} {}",
        app_label.to_ascii_lowercase(),
        process_name.unwrap_or_default().to_ascii_lowercase()
    );

    if contains_any(
        &value,
        &["code", "cursor", "idea", "studio", "terminal", "powershell"],
    ) {
        "coding".to_string()
    } else if contains_any(&value, &["word", "notion", "obsidian", "typora", "onenote"]) {
        "writing".to_string()
    } else if contains_any(&value, &["chrome", "edge", "firefox", "browser"]) {
        "browser".to_string()
    } else if contains_any(&value, &["wechat", "teams", "slack", "discord", "qq"]) {
        "communication".to_string()
    } else if contains_any(&value, &["explorer", "finder"]) {
        "file_management".to_string()
    } else {
        "desktop".to_string()
    }
}

fn classify_activity(category: &str, idle_seconds: i64) -> &'static str {
    if idle_seconds >= 300 {
        "idle"
    } else {
        match category {
            "coding" | "writing" | "file_management" => "productive",
            "communication" => "communication",
            "browser" => "consumption",
            _ => "neutral",
        }
    }
}

fn contains_any(value: &str, needles: &[&str]) -> bool {
    needles.iter().any(|needle| value.contains(needle))
}
