use serde::Serialize;

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ActiveWindowInfo {
    pub app_name: String,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
}

#[cfg(target_os = "windows")]
pub fn get_active_window() -> Result<ActiveWindowInfo, String> {
    use std::path::Path;
    use std::ptr::null_mut;
    use windows::Win32::Foundation::CloseHandle;
    use windows::Win32::System::ProcessStatus::K32GetModuleBaseNameW;
    use windows::Win32::System::Threading::{
        OpenProcess, PROCESS_QUERY_INFORMATION, PROCESS_VM_READ,
    };
    use windows::Win32::UI::WindowsAndMessaging::{
        GetForegroundWindow, GetWindowTextLengthW, GetWindowTextW, GetWindowThreadProcessId,
    };

    unsafe {
        let hwnd = GetForegroundWindow();
        if hwnd.0 == null_mut() {
            return Ok(ActiveWindowInfo::unknown());
        }

        let title = {
            let len = GetWindowTextLengthW(hwnd);
            if len <= 0 {
                None
            } else {
                let mut buffer = vec![0u16; len as usize + 1];
                let copied = GetWindowTextW(hwnd, &mut buffer);
                if copied <= 0 {
                    None
                } else {
                    Some(String::from_utf16_lossy(&buffer[..copied as usize]))
                }
            }
        };

        let mut pid = 0u32;
        GetWindowThreadProcessId(hwnd, Some(&mut pid));
        let process_name = if pid == 0 {
            None
        } else {
            let handle = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false, pid)
                .map_err(|error| format!("open process failed: {error}"))?;
            let mut buffer = vec![0u16; 260];
            let copied = K32GetModuleBaseNameW(handle, None, &mut buffer);
            let _ = CloseHandle(handle);

            if copied == 0 {
                None
            } else {
                let raw = String::from_utf16_lossy(&buffer[..copied as usize]);
                Path::new(&raw)
                    .file_name()
                    .map(|name| name.to_string_lossy().to_string())
                    .or(Some(raw))
            }
        };

        let app_name = process_name
            .as_deref()
            .map(display_app_name)
            .unwrap_or_else(|| "Unknown App".to_string());

        Ok(ActiveWindowInfo {
            app_name,
            window_title: title,
            process_name,
        })
    }
}

#[cfg(not(target_os = "windows"))]
pub fn get_active_window() -> Result<ActiveWindowInfo, String> {
    Ok(ActiveWindowInfo::unknown())
}

impl ActiveWindowInfo {
    fn unknown() -> Self {
        Self {
            app_name: "Unknown App".to_string(),
            window_title: None,
            process_name: None,
        }
    }
}

fn display_app_name(process_name: &str) -> String {
    let stem = process_name
        .trim_end_matches(".exe")
        .trim_end_matches(".EXE")
        .replace('_', " ");

    match stem.to_ascii_lowercase().as_str() {
        "code" => "VS Code".to_string(),
        "chrome" => "Chrome".to_string(),
        "msedge" => "Microsoft Edge".to_string(),
        "firefox" => "Firefox".to_string(),
        "explorer" => "File Explorer".to_string(),
        "powershell" => "PowerShell".to_string(),
        "windowsterminal" => "Windows Terminal".to_string(),
        _ => stem,
    }
}
