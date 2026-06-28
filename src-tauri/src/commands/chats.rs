//! Chat-related Tauri commands.

use tauri::State;
use serde::Deserialize;
use crate::AppState;
use crate::models::chat::ConversationMode;
use crate::storage::{chat_repo, message_repo};

#[tauri::command]
pub async fn list_chats(
    state: State<'_, AppState>,
) -> Result<Vec<crate::models::chat::Chat>, String> {
    chat_repo::list_chats(&state.db.pool)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_chat(
    chat_id: String,
    state: State<'_, AppState>,
) -> Result<Option<crate::models::chat::Chat>, String> {
    chat_repo::list_chats(&state.db.pool)
        .await
        .map(|chats| chats.into_iter().find(|c| c.id.0 == chat_id))
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_messages(
    chat_id: String,
    limit: Option<i64>,
    before: Option<String>,
    state: State<'_, AppState>,
) -> Result<Vec<crate::models::message::Message>, String> {
    let before_dt = before
        .as_deref()
        .and_then(|s| s.parse().ok());
    message_repo::get_messages(&state.db.pool, &chat_id, limit.unwrap_or(50), before_dt)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn archive_chat(
    chat_id: String,
    archived: bool,
    state: State<'_, AppState>,
) -> Result<(), String> {
    chat_repo::set_archived(&state.db.pool, &chat_id, archived)
        .await
        .map_err(|e| e.to_string())
}

#[derive(Deserialize)]
pub struct SetLabelPayload {
    chat_id: String,
    labels: Vec<String>,
}

#[tauri::command]
pub async fn set_chat_label(
    payload: SetLabelPayload,
    state: State<'_, AppState>,
) -> Result<(), String> {
    // Fetch chat, update labels, upsert back
    let mut chats = chat_repo::list_chats(&state.db.pool)
        .await
        .map_err(|e| e.to_string())?;
    if let Some(chat) = chats.iter_mut().find(|c| c.id.0 == payload.chat_id) {
        chat.labels = payload.labels;
        chat_repo::upsert_chat(&state.db.pool, chat)
            .await
            .map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
pub async fn star_message(
    message_id: String,
    starred: bool,
    state: State<'_, AppState>,
) -> Result<(), String> {
    sqlx::query!(
        "UPDATE messages SET is_starred = ? WHERE id = ?",
        starred,
        message_id
    )
    .execute(&state.db.pool)
    .await
    .map(|_| ())
    .map_err(|e| e.to_string())
}
