use tauri::State;
use serde::Deserialize;
use crate::AppState;
use crate::bridge::BridgeRequest;

#[derive(Debug, serde::Serialize)]
pub struct QrResponse {
    pub qr: Option<String>,
    pub status: String,
}

#[tauri::command]
pub async fn get_qr_code(state: State<'_, AppState>) -> Result<QrResponse, String> {
    let resp = state.bridge.sender.send_request(BridgeRequest::GetQr).await.map_err(|e| e.to_string())?;
    let status = state.bridge.status.lock().map(|s| s.clone()).unwrap_or_else(|_| "disconnected".to_string());
    Ok(QrResponse {
        qr: resp.get("qr").and_then(|v| v.as_str()).map(|s| s.to_owned()),
        status,
    })
}

#[tauri::command]
pub async fn get_connection_status(state: State<'_, AppState>) -> Result<String, String> {
    let status = state.bridge.status.lock().map(|s| s.clone()).unwrap_or_else(|_| "disconnected".to_string());
    Ok(status)
}

#[derive(Deserialize)]
pub struct SendTextPayload {
    pub chat_id: String,
    pub text: String,
}

#[tauri::command]
pub async fn send_text_message(payload: SendTextPayload, state: State<'_, AppState>) -> Result<String, String> {
    let resp = state.bridge.sender.send_request(BridgeRequest::SendText {
        jid: payload.chat_id,
        text: payload.text,
    }).await.map_err(|e| e.to_string())?;
    Ok(resp.get("id").and_then(|v| v.as_str()).unwrap_or("").to_owned())
}

#[derive(Deserialize)]
pub struct SendMediaPayload {
    pub chat_id: String,
    pub media_path: String,
    pub caption: Option<String>,
    pub media_type: String,
}

#[tauri::command]
pub async fn send_media_message(payload: SendMediaPayload, state: State<'_, AppState>) -> Result<String, String> {
    let resp = state.bridge.sender.send_request(BridgeRequest::SendMedia {
        jid: payload.chat_id,
        path: payload.media_path,
        caption: payload.caption,
        media_type: payload.media_type,
    }).await.map_err(|e| e.to_string())?;
    Ok(resp.get("id").and_then(|v| v.as_str()).unwrap_or("").to_owned())
}

#[tauri::command]
pub async fn logout(state: State<'_, AppState>) -> Result<(), String> {
    state.bridge.sender.send_request(BridgeRequest::Logout).await.map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub async fn sync_chats(state: State<'_, AppState>) -> Result<serde_json::Value, String> {
    let resp = state.bridge.sender.send_request(BridgeRequest::GetChats).await.map_err(|e| e.to_string())?;
    Ok(resp)
}

#[tauri::command]
pub async fn sync_contacts(state: State<'_, AppState>) -> Result<serde_json::Value, String> {
    let resp = state.bridge.sender.send_request(BridgeRequest::GetContacts).await.map_err(|e| e.to_string())?;
    Ok(resp)
}
