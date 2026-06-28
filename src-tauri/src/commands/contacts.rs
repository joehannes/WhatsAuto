//! Contact-related Tauri commands.

use tauri::State;
use crate::AppState;
use crate::models::contact::Contact;
use crate::storage::contact_repo;

#[tauri::command]
pub async fn list_contacts(
    state: State<'_, AppState>,
) -> Result<Vec<Contact>, String> {
    contact_repo::list_contacts(&state.db.pool)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_contact(
    jid: String,
    state: State<'_, AppState>,
) -> Result<Option<Contact>, String> {
    contact_repo::list_contacts(&state.db.pool)
        .await
        .map(|cs| cs.into_iter().find(|c| c.jid == jid))
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn upsert_contact(
    contact: Contact,
    state: State<'_, AppState>,
) -> Result<(), String> {
    contact_repo::upsert_contact(&state.db.pool, &contact)
        .await
        .map_err(|e| e.to_string())
}
