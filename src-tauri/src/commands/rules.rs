use tauri::State;
use serde_json::Value;
use crate::AppState;

#[tauri::command]
pub async fn list_rules(state: State<'_, AppState>) -> Result<Vec<Value>, String> {
    let rows = sqlx::query!("SELECT id, name, description, is_enabled, conditions, actions, priority, created_at, updated_at FROM rules")
        .fetch_all(&state.db.pool).await.map_err(|e| e.to_string())?;
    let mut result = Vec::new();
    for r in rows {
        result.push(serde_json::json!({
            "id": r.id, "name": r.name, "description": r.description,
            "is_enabled": r.is_enabled != 0, "conditions": r.conditions,
            "actions": r.actions, "priority": r.priority,
            "created_at": r.created_at, "updated_at": r.updated_at
        }));
    }
    Ok(result)
}

#[tauri::command]
pub async fn save_rule(id: String, name: String, description: Option<String>, is_enabled: bool, conditions: String, actions: String, priority: Option<i32>, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("INSERT INTO rules (id, name, description, is_enabled, conditions, actions, priority, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now')) ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description, is_enabled = excluded.is_enabled, conditions = excluded.conditions, actions = excluded.actions, priority = excluded.priority, updated_at = excluded.updated_at",
        id, name, description, is_enabled, conditions, actions, priority.unwrap_or(0)
    ).execute(&state.db.pool).await.map_err(|e| e.to_string())?;
    Ok(())
}
