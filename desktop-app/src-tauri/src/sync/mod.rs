use crate::db;
use rand::Rng;
use serde::{Deserialize, Serialize};
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream, UdpSocket};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex, OnceLock};
use std::thread::{self, JoinHandle};
use std::time::Duration;
use uuid::Uuid;

pub const PAIRING_PROTOCOL: &str = "lifedbg-pairing-v1";
const PAIRING_TTL_MS: i64 = 5 * 60 * 1000;
const DEFAULT_PAIRING_PORT: u16 = 58231;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PairingQrPayload {
    pub protocol: String,
    pub session_id: String,
    pub device_id: String,
    pub device_name: String,
    pub device_type: String,
    pub host: String,
    pub port: u16,
    pub public_key: String,
    pub pairing_token: String,
    pub expires_at: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PairingSessionStatus {
    pub session_id: String,
    pub status: String,
    pub expires_at: i64,
    pub paired_devices: Vec<db::PairedDevice>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
struct PairRequest {
    protocol: String,
    session_id: String,
    pairing_token: String,
    device_id: String,
    device_name: String,
    device_type: String,
    public_key: String,
    timestamp: i64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct PairResponse {
    status: String,
    protocol: Option<String>,
    session_id: Option<String>,
    device_id: Option<String>,
    device_name: Option<String>,
    device_type: Option<String>,
    public_key: Option<String>,
    paired_at: Option<i64>,
    message: String,
    error_code: Option<String>,
}

struct PairingRuntime {
    stop: Arc<AtomicBool>,
    session_id: String,
    port: u16,
    handle: Option<JoinHandle<()>>,
}

static RUNTIME: OnceLock<Mutex<Option<PairingRuntime>>> = OnceLock::new();

pub fn start_pairing_session() -> Result<PairingQrPayload, String> {
    stop_pairing_runtime();

    let conn = db::open_connection()?;
    db::expire_old_pairing_sessions(&conn)?;
    let identity = db::get_or_create_device_identity(&conn)?;
    let host = local_ipv4().unwrap_or_else(|| "127.0.0.1".to_string());
    let listener = bind_pairing_listener()?;
    let port = listener
        .local_addr()
        .map_err(|error| format!("read pairing server port failed: {error}"))?
        .port();

    let now = db::now_ms();
    let session = db::PairingSession {
        session_id: Uuid::new_v4().to_string(),
        pairing_token: db::random_base64(32),
        local_device_id: identity.device_id.clone(),
        local_device_type: identity.device_type.clone(),
        local_host: Some(host.clone()),
        local_port: Some(i64::from(port)),
        expires_at: now + PAIRING_TTL_MS,
        status: "waiting".to_string(),
        created_at: now,
    };
    db::insert_pairing_session(&conn, &session)?;

    let stop = Arc::new(AtomicBool::new(false));
    let thread_stop = Arc::clone(&stop);
    let session_id = session.session_id.clone();
    let handle = thread::spawn(move || run_pairing_server(listener, thread_stop));

    *runtime_lock()? = Some(PairingRuntime {
        stop,
        session_id: session.session_id.clone(),
        port,
        handle: Some(handle),
    });

    Ok(PairingQrPayload {
        protocol: PAIRING_PROTOCOL.to_string(),
        session_id,
        device_id: identity.device_id,
        device_name: identity.device_name,
        device_type: identity.device_type,
        host,
        port,
        public_key: identity.public_key,
        pairing_token: session.pairing_token,
        expires_at: session.expires_at,
    })
}

pub fn cancel_pairing_session(session_id: &str) -> Result<(), String> {
    let conn = db::open_connection()?;
    db::update_pairing_session_status(&conn, session_id, "cancelled")?;
    stop_pairing_runtime();
    Ok(())
}

pub fn pairing_session_status(session_id: &str) -> Result<PairingSessionStatus, String> {
    let conn = db::open_connection()?;
    db::expire_old_pairing_sessions(&conn)?;
    let session = db::get_pairing_session(&conn, session_id)?
        .ok_or_else(|| "pairing session not found".to_string())?;
    if session.status != "waiting" {
        stop_pairing_runtime_if_session(session_id);
    }

    Ok(PairingSessionStatus {
        session_id: session.session_id,
        status: session.status,
        expires_at: session.expires_at,
        paired_devices: db::get_paired_devices(&conn)?,
    })
}

fn run_pairing_server(listener: TcpListener, stop: Arc<AtomicBool>) {
    let _ = listener.set_nonblocking(true);
    while !stop.load(Ordering::Relaxed) {
        match listener.accept() {
            Ok((mut stream, _)) => {
                let should_stop = handle_pair_stream(&mut stream);
                if should_stop {
                    stop.store(true, Ordering::Relaxed);
                }
            }
            Err(error) if error.kind() == std::io::ErrorKind::WouldBlock => {
                thread::sleep(Duration::from_millis(120));
            }
            Err(_) => {
                thread::sleep(Duration::from_millis(120));
            }
        }
    }
}

fn handle_pair_stream(stream: &mut TcpStream) -> bool {
    let mut buffer = vec![0_u8; 16 * 1024];
    let read = match stream.read(&mut buffer) {
        Ok(read) => read,
        Err(_) => return false,
    };
    let request = String::from_utf8_lossy(&buffer[..read]);
    if !request.starts_with("POST /api/pair ") {
        let _ = write_json(stream, 404, error_response("UNKNOWN_ERROR", "Pairing endpoint not found."));
        return false;
    }

    let Some(body_start) = request.find("\r\n\r\n").map(|index| index + 4) else {
        let _ = write_json(stream, 400, error_response("UNKNOWN_ERROR", "Malformed request."));
        return false;
    };
    let body = &request[body_start..];
    let pair_request = match serde_json::from_str::<PairRequest>(body) {
        Ok(value) => value,
        Err(_) => {
            let _ = write_json(stream, 400, error_response("UNKNOWN_ERROR", "Pair request JSON is invalid."));
            return false;
        }
    };

    match validate_and_pair(pair_request) {
        Ok(response) => {
            let _ = write_json(stream, 200, response);
            true
        }
        Err((code, message)) => {
            let _ = write_json(stream, 400, error_response(code, message));
            false
        }
    }
}

fn validate_and_pair(request: PairRequest) -> Result<PairResponse, (&'static str, &'static str)> {
    if request.protocol != PAIRING_PROTOCOL {
        return Err(("INVALID_PROTOCOL", "Pairing protocol is not supported."));
    }
    if request.device_type != "android" {
        return Err(("DEVICE_TYPE_NOT_ALLOWED", "Only Android devices can use this pairing flow."));
    }
    if request.public_key.trim().is_empty() {
        return Err(("PUBLIC_KEY_MISSING", "Paired device public key is missing."));
    }
    if request.device_id.trim().is_empty() || request.device_name.trim().is_empty() || request.timestamp <= 0 {
        return Err(("UNKNOWN_ERROR", "Pair request is incomplete."));
    }

    let conn = db::open_connection().map_err(|_| ("UNKNOWN_ERROR", "Cannot open local database."))?;
    db::expire_old_pairing_sessions(&conn).map_err(|_| ("UNKNOWN_ERROR", "Cannot update pairing session."))?;
    let session = db::get_pairing_session(&conn, &request.session_id)
        .map_err(|_| ("UNKNOWN_ERROR", "Cannot read pairing session."))?
        .ok_or(("SESSION_NOT_FOUND", "Pairing session was not found."))?;

    if session.status != "waiting" {
        return Err(("SESSION_NOT_WAITING", "Pairing session is no longer waiting."));
    }
    if session.expires_at <= db::now_ms() {
        let _ = db::update_pairing_session_status(&conn, &request.session_id, "expired");
        return Err(("TOKEN_EXPIRED", "Pairing token has expired."));
    }
    if session.pairing_token != request.pairing_token {
        return Err(("TOKEN_INVALID", "Pairing token is invalid."));
    }
    if db::paired_device_exists(&conn, &request.device_id)
        .map_err(|_| ("UNKNOWN_ERROR", "Cannot check paired device."))?
    {
        return Err(("ALREADY_PAIRED", "This device is already paired."));
    }

    let local_identity =
        db::get_or_create_device_identity(&conn).map_err(|_| ("UNKNOWN_ERROR", "Cannot read local identity."))?;
    let paired_at = db::now_ms();
    db::upsert_paired_device(
        &conn,
        &db::PairedDevice {
            paired_device_id: request.device_id,
            paired_device_name: request.device_name,
            paired_device_type: request.device_type,
            public_key: request.public_key,
            last_known_host: None,
            last_known_port: None,
            pairing_status: "paired".to_string(),
            trust_level: "trusted".to_string(),
            paired_at,
            last_seen_at: Some(paired_at),
            last_sync_at: None,
            metadata_json: None,
        },
    )
    .map_err(|_| ("UNKNOWN_ERROR", "Cannot save paired device."))?;
    db::update_pairing_session_status(&conn, &request.session_id, "paired")
        .map_err(|_| ("UNKNOWN_ERROR", "Cannot mark pairing session as paired."))?;

    Ok(PairResponse {
        status: "ok".to_string(),
        protocol: Some(PAIRING_PROTOCOL.to_string()),
        session_id: Some(request.session_id),
        device_id: Some(local_identity.device_id),
        device_name: Some(local_identity.device_name),
        device_type: Some(local_identity.device_type),
        public_key: Some(local_identity.public_key),
        paired_at: Some(paired_at),
        message: "paired".to_string(),
        error_code: None,
    })
}

fn error_response(error_code: &'static str, message: &'static str) -> PairResponse {
    PairResponse {
        status: "error".to_string(),
        protocol: None,
        session_id: None,
        device_id: None,
        device_name: None,
        device_type: None,
        public_key: None,
        paired_at: None,
        message: message.to_string(),
        error_code: Some(error_code.to_string()),
    }
}

fn write_json<T: Serialize>(stream: &mut TcpStream, status: u16, value: T) -> std::io::Result<()> {
    let body = serde_json::to_string(&value).unwrap_or_else(|_| "{}".to_string());
    let status_text = if status == 200 { "OK" } else { "Error" };
    write!(
        stream,
        "HTTP/1.1 {status} {status_text}\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
        body.as_bytes().len(),
        body
    )
}

fn bind_pairing_listener() -> Result<TcpListener, String> {
    if let Ok(listener) = TcpListener::bind(("0.0.0.0", DEFAULT_PAIRING_PORT)) {
        return Ok(listener);
    }

    let mut rng = rand::thread_rng();
    for _ in 0..80 {
        let port = rng.gen_range(50000..=60000);
        match TcpListener::bind(("0.0.0.0", port)) {
            Ok(listener) => return Ok(listener),
            Err(_) => continue,
        }
    }
    TcpListener::bind(("0.0.0.0", 0)).map_err(|error| format!("start pairing server failed: {error}"))
}

fn local_ipv4() -> Option<String> {
    let socket = UdpSocket::bind("0.0.0.0:0").ok()?;
    socket.connect("8.8.8.8:80").ok()?;
    let address = socket.local_addr().ok()?;
    Some(address.ip().to_string())
}

fn runtime_lock() -> Result<std::sync::MutexGuard<'static, Option<PairingRuntime>>, String> {
    RUNTIME
        .get_or_init(|| Mutex::new(None))
        .lock()
        .map_err(|_| "pairing runtime lock poisoned".to_string())
}

fn stop_pairing_runtime() {
    if let Ok(mut runtime) = runtime_lock() {
        if let Some(mut value) = runtime.take() {
            value.stop.store(true, Ordering::Relaxed);
            let _ = TcpStream::connect(("127.0.0.1", value.port));
            if let Some(handle) = value.handle.take() {
                let _ = handle.join();
            }
        }
    }
}

fn stop_pairing_runtime_if_session(session_id: &str) {
    if let Ok(runtime) = runtime_lock() {
        if runtime
            .as_ref()
            .map(|value| value.session_id == session_id)
            .unwrap_or(false)
        {
            drop(runtime);
            stop_pairing_runtime();
        }
    }
}
