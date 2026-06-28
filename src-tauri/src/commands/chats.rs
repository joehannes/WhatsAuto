use tauri::State;
use serde::Deserialize;
use crate::AppState;
use crate::models::chat::{Chat, ConversationMode};
use crate::storage::chat_repo;

#[tauri::command]
pub async fn list_chats(state: State<'_, AppState>) -> Result<Vec<Chat>, String> {
    chat_repo::list_chats(&state.db.pool).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_chat(chat_id: String, state: State<'_, AppState>) -> Result<Option<Chat>, String> {
    let chats = chat_repo::list_chats(&state.db.pool).await.map_err(|e| e.to_string())?;
    Ok(chats.into_iter().find(|c| c.id.0 == chat_id))
}

#[tauri::command]
pub async fn archive_chat(chat_id: String, archived: bool, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("UPDATE chats SET is_archived = ? WHERE id = ?", archived, chat_id).execute(&state.db.pool).await
        .map(|_| ()).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn pin_chat(chat_id: String, pinned: bool, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("UPDATE chats SET is_pinned = ? WHERE id = ?", pinned, chat_id).execute(&state.db.pool).await
        .map(|_| ()).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn set_chat_conversation_mode(chat_id: String, mode: String, state: State<'_, AppState>) -> Result<(), String> {
    let mode_enum = match mode.as_str() {
        "assisted" => ConversationMode::Assisted,
        "autonomous" => ConversationMode::Autonomous,
        _ => ConversationMode::Human,
    };
    sqlx::query!("UPDATE chats SET conversation_mode = ? WHERE id = ?", mode, chat_id).execute(&state.db.pool).await
        .map(|_| ()).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn star_message(message_id: String, starred: bool, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("UPDATE messages SET is_starred = ? WHERE id = ?", starred, message_id).execute(&state.db.pool).await
        .map(|_| ()).map_err(|e| e.to_string())
}
