use anyhow::{bail, Context, Result};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter};
use tokio::sync::{mpsc, oneshot};
use tokio::time::{sleep, Duration};
use tokio_tungstenite::{accept_async, tungstenite::Message as WsMessage};
use tracing::{debug, error, info, warn};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize)]
#[serde(tag = "cmd", rename_all = "snake_case")]
pub enum BridgeRequest {
    GetQr,
    GetStatus,
    SendText { jid: String, text: String },
    SendMedia { jid: String, path: String, caption: Option<String>, media_type: String },
    SendReaction { jid: String, message_id: String, emoji: String },
    Logout,
    GetChats,
    GetMessages { jid: String, limit: i64 },
    GetContacts,
    SendPresence { jid: String, is_typing: bool },
    GetChatInfo { jid: String },
}

#[derive(Debug, Clone, Deserialize)]
#[serde(tag = "event", rename_all = "snake_case")]
pub enum SidecarEvent {
    QrCode { qr: String },
    Authenticated,
    Disconnected { reason: Option<String> },
    MessageReceived { message: Value },
    MessageAck { id: String, ack: i32 },
    MessageReaction { id: String, emoji: String, sender_id: String },
    TypingStart { jid: String },
    TypingStop { jid: String },
    PresenceUpdate { jid: String, status: String },
    ContactsSync { contacts: Vec<Value> },
    ChatsSync { chats: Vec<Value> },
    ChatUpdate { id: String, pinned: bool, archived: bool, muted: bool },
}

type PendingMap = Arc<Mutex<HashMap<String, oneshot::Sender<Value>>>>;

#[derive(Clone)]
pub struct BridgeSender {
    tx: mpsc::Sender<(String, BridgeRequest, oneshot::Sender<Value>)>,
}

impl BridgeSender {
    pub async fn send_request(&self, req: BridgeRequest) -> Result<Value> {
        let (resp_tx, resp_rx) = oneshot::channel();
        let id = Uuid::new_v4().to_string();
        self.tx.send((id, req, resp_tx)).await.context("bridge channel closed")?;
        resp_rx.await.context("bridge response closed")
    }
}

pub struct BridgeState {
    pub sender: BridgeSender,
    pub status: Arc<Mutex<String>>,
}

pub async fn start(app: AppHandle) -> Result<BridgeState> {
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await?;
    let port = listener.local_addr()?.port();
    info!("Bridge WS server on port {}", port);

    let (tx, mut rx) = mpsc::channel::<(String, BridgeRequest, oneshot::Sender<Value>)>(64);
    let pending: PendingMap = Arc::new(Mutex::new(HashMap::new()));
    let status = Arc::new(Mutex::new("disconnected".to_string()));

    let pending_clone = pending.clone();
    let app_clone = app.clone();
    let status_clone = status.clone();

    tokio::spawn(async move {
        match listener.accept().await {
            Ok((stream, addr)) => {
                info!("Sidecar connected from {}", addr);
                match accept_async(stream).await {
                    Ok(ws) => run_bridge(ws, rx, pending_clone, app_clone, status_clone).await,
                    Err(e) => error!("WS handshake failed: {}", e),
                }
            }
            Err(e) => error!("Accept error: {}", e),
        }
    });

    launch_sidecar(&app, port)?;

    Ok(BridgeState {
        sender: BridgeSender { tx },
        status,
    })
}

fn launch_sidecar(app: &AppHandle, port: u16) -> Result<()> {
    use tauri_plugin_shell::ShellExt;
    let sidecar = app.shell().sidecar("wa-sidecar").context("sidecar not found")?;
    let (mut rx, _child) = sidecar.args(["--port", &port.to_string()]).spawn().context("spawn failed")?;

    info!("wa-sidecar spawned on port {}", port);

    tokio::spawn(async move {
        while let Some(event) = rx.recv().await {
            use tauri_plugin_shell::process::CommandEvent;
            match event {
                CommandEvent::Stdout(line) => debug!("[sidecar] {}", String::from_utf8_lossy(&line)),
                CommandEvent::Stderr(line) => warn!("[sidecar] {}", String::from_utf8_lossy(&line)),
                CommandEvent::Error(e) => error!("[sidecar] error: {}", e),
                CommandEvent::Terminated(s) => warn!("[sidecar] terminated: {:?}", s),
                _ => {}
            }
        }
    });

    Ok(())
}

async fn run_bridge(
    ws: tokio_tungstenite::WebSocketStream<tokio::net::TcpStream>,
    mut outgoing: mpsc::Receiver<(String, BridgeRequest, oneshot::Sender<Value>)>,
    pending: PendingMap,
    app: AppHandle,
    status: Arc<Mutex<String>>,
) {
    let (mut write, mut read) = ws.split();
    let pending_read = pending.clone();
    let status_read = status.clone();

    let app_read = app.clone();
    tokio::spawn(async move {
        while let Some(msg) = read.next().await {
            match msg {
                Ok(WsMessage::Text(text)) => {
                    if let Ok(value) = serde_json::from_str::<Value>(&text) {
                        if let Some(id) = value.get("id").and_then(|v| v.as_str()) {
                            let mut guard = pending_read.lock().unwrap();
                            if let Some(tx) = guard.remove(id) {
                                let _ = tx.send(value.clone());
                                continue;
                            }
                        }

                        if let Some(event) = value.get("event").and_then(|v| v.as_str()) {
                            if event == "authenticated" {
                                if let Ok(mut s) = status_read.lock() {
                                    *s = "connected".to_string();
                                }
                            } else if event == "disconnected" {
                                if let Ok(mut s) = status_read.lock() {
                                    *s = "disconnected".to_string();
                                }
                            }

                            let event_key = format!("wa:{}", event);
                            if let Err(e) = app_read.emit(&event_key, &value) {
                                warn!("Emit failed {}: {}", event_key, e);
                            }
                        }
                    }
                }
                Ok(WsMessage::Close(_)) => { info!("Sidecar closed"); break; }
                Err(e) => { error!("Bridge read error: {}", e); break; }
                _ => {}
            }
        }
    });

    while let Some((id, req, resp_tx)) = outgoing.recv().await {
        let mut payload = serde_json::to_value(&req).unwrap_or(json!({}));
        payload["id"] = json!(id);
        {
            let mut guard = pending.lock().unwrap();
            guard.insert(id.clone(), resp_tx);
        }
        if let Err(e) = write.send(WsMessage::Text(payload.to_string().into())).await {
            error!("Bridge write error: {}", e);
            let mut guard = pending.lock().unwrap();
            guard.remove(&id);
            break;
        }
    }
}
