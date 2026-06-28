use tauri::State;
use crate::AppState;
use crate::models::ai::PromptTemplate;
use crate::storage::prompt_repo;

#[tauri::command]
pub async fn list_prompt_templates(state: State<'_, AppState>) -> Result<Vec<PromptTemplate>, String> {
    prompt_repo::list_templates(&state.db.pool).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn save_prompt_template(template: PromptTemplate, state: State<'_, AppState>) -> Result<String, String> {
    prompt_repo::save_template(&state.db.pool, &template).await.map_err(|e| e.to_string())
}
