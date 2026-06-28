use tauri::State;
use serde_json::Value;
use crate::AppState;

#[tauri::command]
pub async fn list_scheduled_tasks(state: State<'_, AppState>) -> Result<Vec<Value>, String> {
    let rows = sqlx::query!("SELECT id, chat_id, task_type, payload, status, scheduled_at, executed_at, retry_count, created_at FROM scheduled_tasks WHERE status = 'pending' OR status = 'running' ORDER BY scheduled_at")
        .fetch_all(&state.db.pool).await.map_err(|e| e.to_string())?;
    let mut result = Vec::new();
    for r in rows {
        result.push(serde_json::json!({
            "id": r.id, "chat_id": r.chat_id, "task_type": r.task_type,
            "payload": r.payload, "status": r.status, "scheduled_at": r.scheduled_at,
            "executed_at": r.executed_at, "retry_count": r.retry_count,
            "created_at": r.created_at
        }));
    }
    Ok(result)
}

#[tauri::command]
pub async fn schedule_task(id: String, chat_id: Option<String>, task_type: String, payload: String, scheduled_at: String, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("INSERT INTO scheduled_tasks (id, chat_id, task_type, payload, status, scheduled_at, created_at) VALUES (?, ?, ?, ?, 'pending', ?, datetime('now'))",
        id, chat_id, task_type, payload, scheduled_at
    ).execute(&state.db.pool).await.map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub async fn cancel_task(id: String, state: State<'_, AppState>) -> Result<(), String> {
    sqlx::query!("UPDATE scheduled_tasks SET status = 'cancelled' WHERE id = ?", id).execute(&state.db.pool).await
        .map_err(|e| e.to_string())?;
    Ok(())
}
