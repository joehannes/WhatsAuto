//! WhatsApp-related Tauri commands.

use tauri::State;
use serde::{Deserialize, Serialize};
use crate::AppState;
use crate::bridge::{BridgeRequest, BridgeSender};

#[derive(Debug, Serialize)]
pub struct QrCodeResponse {
    pub qr: Option<String>,
    pub status: String,
}

#[tauri::command]
pub async fn get_qr_code(state: State<'_, AppState>) -> Result<QrCodeResponse, String> {
    let resp = state
        .bridge_tx
        .send_request(BridgeRequest::GetQr)
        .await
        .map_err(|e| e.to_string())?;

    Ok(QrCodeResponse {
        qr: resp.get("qr").and_then(|v| v.as_str()).map(|s| s.to_owned()),
        status: resp
            .get("status")
            .and_then(|v| v.as_str())
            .unwrap_or("disconnected")
            .to_owned(),
    })
}

#[tauri::command]
pub async fn get_connection_status(state: State<'_, AppState>) -> Result<String, String> {
    let resp = state
        .bridge_tx
        .send_request(BridgeRequest::GetStatus)
        .await
        .map_err(|e| e.to_string())?;

    Ok(resp
        .get("status")
        .and_then(|v| v.as_str())
        .unwrap_or("disconnected")
        .to_owned())
}

#[derive(Debug, Deserialize)]
pub struct SendTextPayload {
    pub chat_id: String,
    pub text: String,
}

#[tauri::command]
pub async fn send_text_message(
    payload: SendTextPayload,
    state: State<'_, AppState>,
) -> Result<String, String> {
    let req = BridgeRequest::SendText {
        jid: payload.chat_id.clone(),
        text: payload.text.clone(),
    };
    let resp = state
        .bridge_tx
        .send_request(req)
        .await
        .map_err(|e| e.to_string())?;

    Ok(resp
        .get("id")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_owned())
}

#[derive(Debug, Deserialize)]
pub struct SendMediaPayload {
    pub chat_id: String,
    pub media_path: String,
    pub caption: Option<String>,
    pub media_type: String,
}

#[tauri::command]
pub async fn send_media_message(
    payload: SendMediaPayload,
    state: State<'_, AppState>,
) -> Result<String, String> {
    let req = BridgeRequest::SendMedia {
        jid: payload.chat_id,
        path: payload.media_path,
        caption: payload.caption,
        media_type: payload.media_type,
    };
    let resp = state
        .bridge_tx
        .send_request(req)
        .await
        .map_err(|e| e.to_string())?;

    Ok(resp
        .get("id")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_owned())
}

#[tauri::command]
pub async fn logout(state: State<'_, AppState>) -> Result<(), String> {
    state
        .bridge_tx
        .send_request(BridgeRequest::Logout)
        .await
        .map_err(|e| e.to_string())?;
    Ok(())
}
