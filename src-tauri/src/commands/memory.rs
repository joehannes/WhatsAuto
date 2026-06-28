use tauri::State;
use crate::AppState;
use crate::models::ai::ConversationMemory;
use crate::storage::prompt_repo;

#[tauri::command]
pub async fn get_conversation_memory(chat_id: String, state: State<'_, AppState>) -> Result<Option<ConversationMemory>, String> {
    prompt_repo::get_memory(&state.db.pool, &chat_id).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn save_conversation_memory(memory: ConversationMemory, state: State<'_, AppState>) -> Result<(), String> {
    prompt_repo::save_memory(&state.db.pool, &memory).await.map_err(|e| e.to_string())
}
