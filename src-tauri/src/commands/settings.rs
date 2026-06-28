//! Settings Tauri commands.

use tauri::State;
use crate::AppState;
use crate::models::settings::Settings;
use crate::storage::settings_repo;

#[tauri::command]
pub async fn get_settings(
    state: State<'_, AppState>,
) -> Result<Settings, String> {
    settings_repo::get_settings(&state.db.pool)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn save_settings(
    settings: Settings,
    state: State<'_, AppState>,
) -> Result<(), String> {
    settings_repo::save_settings(&state.db.pool, &settings)
        .await
        .map_err(|e| e.to_string())
}
