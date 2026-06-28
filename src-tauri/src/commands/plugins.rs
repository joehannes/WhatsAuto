use tauri::State;
use serde_json::Value;
use crate::AppState;

#[tauri::command]
pub async fn list_plugins(state: State<'_, AppState>) -> Result<Vec<Value>, String> {
    let rows = sqlx::query!("SELECT id, name, version, kind, config, is_enabled, created_at FROM plugins")
        .fetch_all(&state.db.pool).await.map_err(|e| e.to_string())?;
    let mut result = Vec::new();
    for r in rows {
        result.push(serde_json::json!({
            "id": r.id, "name": r.name, "version": r.version,
            "kind": r.kind, "config": r.config, "is_enabled": r.is_enabled != 0,
            "created_at": r.created_at
        }));
    }
    Ok(result)
}

#[tauri::command]
pub async fn register_plugin(id: String, name: String, version: String, kind: String, config: Option<String>, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("INSERT INTO plugins (id, name, version, kind, config, is_enabled, created_at) VALUES (?, ?, ?, ?, ?, 0, datetime('now')) ON CONFLICT(id) DO UPDATE SET name = excluded.name, version = excluded.version, kind = excluded.kind, config = excluded.config",
        id, name, version, kind, config.unwrap_or_else(|| "{}".to_string())
    ).execute(&state.db.pool).await.map_err(|e| e.to_string())?;
    Ok(())
}
