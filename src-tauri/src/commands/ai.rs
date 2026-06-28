//! AI-related Tauri commands.

use tauri::State;
use serde::{Deserialize, Serialize};
use crate::AppState;
use crate::models::ai::{AiMessage, AiProvider, AiResponse};
use crate::models::chat::ConversationMode;
use crate::services::ai as ai_service;
use crate::storage::{ai_repo, chat_repo};

#[tauri::command]
pub async fn list_providers(
    state: State<'_, AppState>,
) -> Result<Vec<AiProvider>, String> {
    ai_repo::list_providers(&state.db.pool)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn save_provider(
    provider: AiProvider,
    api_key: Option<String>,
    state: State<'_, AppState>,
) -> Result<String, String> {
    // Persist API key to OS keychain if provided
    if let Some(key) = api_key {
        if !key.is_empty() {
            let key_ref = format!("whatsauto-ai-{}", &provider.id);
            keyring::Entry::new("whatsauto", &key_ref)
                .map_err(|e| e.to_string())?
                .set_password(&key)
                .map_err(|e| e.to_string())?;
        }
    }

    ai_repo::save_provider(&state.db.pool, &provider)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn delete_provider(
    id: String,
    state: State<'_, AppState>,
) -> Result<(), String> {
    ai_repo::delete_provider(&state.db.pool, &id)
        .await
        .map_err(|e| e.to_string())
}

#[derive(Debug, Deserialize)]
pub struct AiChatPayload {
    pub provider_id: String,
    pub messages: Vec<AiMessage>,
}

#[tauri::command]
pub async fn send_ai_message(
    payload: AiChatPayload,
    state: State<'_, AppState>,
) -> Result<AiResponse, String> {
    let providers = ai_repo::list_providers(&state.db.pool)
        .await
        .map_err(|e| e.to_string())?;

    let provider = providers
        .into_iter()
        .find(|p| p.id == payload.provider_id)
        .ok_or_else(|| "Provider not found".to_string())?;

    ai_service::chat(&provider, payload.messages)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_conversation_mode(
    chat_id: String,
    state: State<'_, AppState>,
) -> Result<ConversationMode, String> {
    chat_repo::list_chats(&state.db.pool)
        .await
        .map(|chats| {
            chats
                .into_iter()
                .find(|c| c.id.0 == chat_id)
                .map(|c| c.conversation_mode)
                .unwrap_or_default()
        })
        .map_err(|e| e.to_string())
}

#[derive(Deserialize)]
pub struct SetModePayload {
    pub chat_id: String,
    pub mode: ConversationMode,
}

#[tauri::command]
pub async fn set_conversation_mode(
    payload: SetModePayload,
    state: State<'_, AppState>,
) -> Result<(), String> {
    chat_repo::set_conversation_mode(&state.db.pool, &payload.chat_id, &payload.mode)
        .await
        .map_err(|e| e.to_string())
}
