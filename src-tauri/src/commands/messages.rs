use tauri::State;
use serde::Deserialize;
use crate::AppState;
use crate::models::message::Message;
use crate::storage::message_repo;

#[tauri::command]
pub async fn get_messages(chat_id: String, limit: Option<i64>, state: State<'_, AppState>) -> Result<Vec<Message>, String> {
    message_repo::get_messages(&state.db.pool, &chat_id, limit.unwrap_or(50)).await.map_err(|e| e.to_string())
}

#[derive(Deserialize)]
pub struct SendReactionPayload {
    pub jid: String,
    pub message_id: String,
    pub emoji: String,
}

#[tauri::command]
pub async fn send_reaction(payload: SendReactionPayload, state: State<'_, AppState>) -> Result<(), String> {
    let req = crate::bridge::BridgeRequest::SendReaction {
        jid: payload.jid,
        message_id: payload.message_id,
        emoji: payload.emoji,
    };
    state.bridge.sender.send_request(req).await.map_err(|e| e.to_string())?;
    Ok(())
}
