use tauri::State;
use serde::Deserialize;
use crate::AppState;
use crate::models::ai::{AiMessage, AiProvider, AiResponse};
use crate::services::ai as ai_service;
use crate::storage::ai_repo;

#[tauri::command]
pub async fn list_providers(state: State<'_, AppState>) -> Result<Vec<AiProvider>, String> {
    ai_repo::list_providers(&state.db.pool).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn save_provider(provider: AiProvider, api_key: Option<String>, state: State<'_, AppState>) -> Result<String, String> {
    if let Some(key) = api_key {
        if !key.is_empty() {
            let key_ref = format!("whatsauto-ai-{}", &provider.id);
            keyring::Entry::new("whatsauto", &key_ref).map_err(|e| e.to_string())?
                .set_password(&key).map_err(|e| e.to_string())?;
        }
    }
    ai_repo::save_provider(&state.db.pool, &provider).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn delete_provider(id: String, state: State<'_, AppState>) -> Result<(), String> {
    ai_repo::delete_provider(&state.db.pool, &id).await.map_err(|e| e.to_string())
}

#[derive(Deserialize)]
pub struct AiChatPayload {
    pub provider_id: String,
    pub messages: Vec<AiMessage>,
    pub stream: Option<bool>,
}

#[tauri::command]
pub async fn send_ai_message(payload: AiChatPayload, state: State<'_, AppState>) -> Result<AiResponse, String> {
    let providers = ai_repo::list_providers(&state.db.pool).await.map_err(|e| e.to_string())?;
    let provider = providers.into_iter().find(|p| p.id == payload.provider_id)
        .ok_or_else(|| "Provider not found".to_string())?;
    ai_service::chat(&provider, payload.messages, payload.stream.unwrap_or(false)).await
        .map_err(|e| e.to_string())
}
