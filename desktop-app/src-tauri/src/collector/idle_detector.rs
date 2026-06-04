#[cfg(target_os = "windows")]
pub fn get_idle_seconds() -> Result<i64, String> {
    use windows::Win32::System::SystemInformation::GetTickCount64;
    use windows::Win32::UI::Input::KeyboardAndMouse::{GetLastInputInfo, LASTINPUTINFO};

    unsafe {
        let mut info = LASTINPUTINFO {
            cbSize: std::mem::size_of::<LASTINPUTINFO>() as u32,
            dwTime: 0,
        };

        if !GetLastInputInfo(&mut info).as_bool() {
            return Err("get last input info failed".to_string());
        }

        let now_ms = GetTickCount64();
        let last_input_ms = info.dwTime as u64;
        Ok(now_ms.saturating_sub(last_input_ms) as i64 / 1000)
    }
}

#[cfg(not(target_os = "windows"))]
pub fn get_idle_seconds() -> Result<i64, String> {
    Ok(0)
}
