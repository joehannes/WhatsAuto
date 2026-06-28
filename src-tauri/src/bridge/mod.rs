//! Bridge module — manages the Node.js sidecar lifecycle and IPC.
//!
//! Architecture:
//! - The Node.js sidecar is launched as a Tauri sidecar process.
//! - Communication uses a WebSocket on localhost (chosen at startup).
//! - The sidecar speaks a simple JSON-RPC-style protocol.
//! - Incoming events from whatsapp-web.js are forwarded to the Tauri
//!   frontend via Tauri event emission.

use anyhow::{bail, Context, Result};
use futures_util::{SinkExt, StreamExt};
use once_cell::sync::OnceCell;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
};
use tauri::{AppHandle, Emitter};
use tokio::{
    net::TcpListener,
    sync::{mpsc, oneshot},
    time::{sleep, Duration},
};
use tokio_tungstenite::{
    accept_async,
    tungstenite::Message as WsMessage,
};
use tracing::{debug, error, info, warn};
use uuid::Uuid;

// ============================================================
// Public request/response types
// ============================================================

/// Requests that Rust can send to the sidecar.
#[derive(Debug, Clone, Serialize)]
#[serde(tag = "cmd", rename_all = "snake_case")]
pub enum BridgeRequest {
    GetQr,
    GetStatus,
    SendText { jid: String, text: String },
    SendMedia { jid: String, path: String, caption: Option<String>, media_type: String },
    Logout,
    GetChats,
    GetMessages { jid: String, limit: i64 },
}

/// Events emitted from the sidecar to Rust / frontend.
#[derive(Debug, Clone, Deserialize)]
#[serde(tag = "event", rename_all = "snake_case")]
pub enum SidecarEvent {
    QrCode { qr: String },
    Authenticated,
    Disconnected { reason: Option<String> },
    MessageReceived { message: Value },
    MessageAck { id: String, ack: i32 },
    TypingStart { jid: String },
    TypingStop { jid: String },
    ContactsSync { contacts: Vec<Value> },
    ChatsSync { chats: Vec<Value> },
}

// ============================================================
// Bridge sender (cloneable handle to the bridge)
// ============================================================

type PendingMap = Arc<Mutex<HashMap<String, oneshot::Sender<Value>>>>;

/// A cloneable handle to send requests to the sidecar and await responses.
#[derive(Clone)]
pub struct BridgeSender {
    tx: mpsc::Sender<(String, BridgeRequest, oneshot::Sender<Value>)>,
}

impl BridgeSender {
    /// Send a request to the sidecar and await the JSON response.
    pub async fn send_request(&self, req: BridgeRequest) -> Result<Value> {
        let (resp_tx, resp_rx) = oneshot::channel();
        let id = Uuid::new_v4().to_string();
        self.tx
            .send((id, req, resp_tx))
            .await
            .context("bridge channel closed")?;
        resp_rx.await.context("bridge response channel closed")
    }
}

// ============================================================
// Bridge startup
// ============================================================

/// Start the WebSocket bridge server, launch the Node.js sidecar,
/// and return a `BridgeSender` for sending requests.
pub async fn start(app: AppHandle) -> Result<BridgeSender> {
    // Bind on a random ephemeral port
    let listener = TcpListener::bind("127.0.0.1:0").await?;
    let port = listener.local_addr()?.port();
    info!("Bridge WebSocket server listening on port {}", port);

    let (tx, mut rx) = mpsc::channel::<(String, BridgeRequest, oneshot::Sender<Value>)>(64);
    let pending: PendingMap = Arc::new(Mutex::new(HashMap::new()));

    let pending_clone = pending.clone();
    let app_clone = app.clone();

    // Spawn the bridge accept loop
    tokio::spawn(async move {
        match listener.accept().await {
            Ok((stream, addr)) => {
                info!("Sidecar connected from {}", addr);
                match accept_async(stream).await {
                    Ok(ws) => {
                        run_bridge(ws, rx, pending_clone, app_clone).await;
                    }
                    Err(e) => error!("WebSocket handshake failed: {}", e),
                }
            }
            Err(e) => error!("Bridge accept error: {}", e),
        }
    });

    // Launch the Node.js sidecar with the port as argument
    launch_sidecar(&app, port)?;

    Ok(BridgeSender { tx })
}

/// Launch the Node.js sidecar process.
fn launch_sidecar(app: &AppHandle, port: u16) -> Result<()> {
    use tauri_plugin_shell::ShellExt;
    
    let sidecar = app
        .shell()
        .sidecar("wa-sidecar")
        .context("sidecar binary not found")?;

    let (mut rx, _child) = sidecar
        .args(["--port", &port.to_string()])
        .spawn()
        .context("failed to spawn sidecar")?;

    info!("wa-sidecar spawned on bridge port {}", port);

    // Log sidecar stdout/stderr in background
    let app_clone = app.clone();
    tokio::spawn(async move {
        while let Some(event) = rx.recv().await {
            use tauri_plugin_shell::process::CommandEvent;
            match event {
                CommandEvent::Stdout(line) => {
                    debug!("[sidecar] {}", String::from_utf8_lossy(&line));
                }
                CommandEvent::Stderr(line) => {
                    warn!("[sidecar err] {}", String::from_utf8_lossy(&line));
                }
                CommandEvent::Error(e) => {
                    error!("[sidecar] process error: {}", e);
                }
                CommandEvent::Terminated(s) => {
                    warn!("[sidecar] terminated: {:?}", s);
                }
                _ => {}
            }
        }
    });

    Ok(())
}

// ============================================================
// Bridge run loop — handles bidirectional message flow
// ============================================================

async fn run_bridge(
    ws: tokio_tungstenite::WebSocketStream<tokio::net::TcpStream>,
    mut outgoing: mpsc::Receiver<(String, BridgeRequest, oneshot::Sender<Value>)>,
    pending: PendingMap,
    app: AppHandle,
) {
    let (mut write, mut read) = ws.split();

    let pending_read = pending.clone();

    // Task: forward incoming messages from sidecar → Rust / frontend events
    let app_read = app.clone();
    tokio::spawn(async move {
        while let Some(msg) = read.next().await {
            match msg {
                Ok(WsMessage::Text(text)) => {
                    if let Ok(value) = serde_json::from_str::<Value>(&text) {
                        // If this is a response to a pending request, resolve it
                        if let Some(id) = value.get("id").and_then(|v| v.as_str()) {
                            let mut guard = pending_read.lock().unwrap();
                            if let Some(tx) = guard.remove(id) {
                                let _ = tx.send(value.clone());
                                continue;
                            }
                        }

                        // Otherwise treat as an event and emit to frontend
                        let event_name = value
                            .get("event")
                            .and_then(|v| v.as_str())
                            .unwrap_or("wa:unknown")
                            .to_owned();

                        let event_key = format!("wa:{}", event_name);
                        if let Err(e) = app_read.emit(&event_key, &value) {
                            warn!("Failed to emit event {}: {}", event_key, e);
                        }
                    }
                }
                Ok(WsMessage::Close(_)) => {
                    info!("Sidecar closed WebSocket connection");
                    break;
                }
                Err(e) => {
                    error!("Bridge read error: {}", e);
                    break;
                }
                _ => {}
            }
        }
    });

    // Task: forward outgoing requests Rust → sidecar
    while let Some((id, req, resp_tx)) = outgoing.recv().await {
        let mut payload = serde_json::to_value(&req).unwrap_or(json!({}));
        payload["id"] = json!(id);

        {
            let mut guard = pending.lock().unwrap();
            guard.insert(id.clone(), resp_tx);
        }

        let text = payload.to_string();
        if let Err(e) = write.send(WsMessage::Text(text.into())).await {
            error!("Bridge write error: {}", e);
            // Remove from pending and signal error
            let mut guard = pending.lock().unwrap();
            guard.remove(&id);
            break;
        }
    }
}
