use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::ai::{PromptTemplate, ConversationMemory};

pub async fn list_templates(pool: &SqlitePool) -> Result<Vec<PromptTemplate>> {
    let rows = sqlx::query!("SELECT id, name, description, category, tags, template, variables, provider_id, is_builtin, created_at, updated_at FROM prompt_templates ORDER BY name")
        .fetch_all(pool).await?;
    Ok(rows.into_iter().map(|r| PromptTemplate {
        id: r.id, name: r.name, description: r.description, category: r.category,
        tags: r.tags.map(|s| serde_json::from_str(&s).unwrap_or_default()).unwrap_or_default(),
        template: r.template,
        variables: r.variables.map(|s| serde_json::from_str(&s).unwrap_or_default()).unwrap_or_default(),
        provider_id: r.provider_id, is_builtin: r.is_builtin != 0,
        created_at: r.created_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
        updated_at: r.updated_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
    }).collect())
}

pub async fn save_template(pool: &SqlitePool, t: &PromptTemplate) -> Result<String> {
    let id = if t.id.is_empty() { uuid::Uuid::new_v4().to_string() } else { t.id.clone() };
    let tags = serde_json::to_string(&t.tags)?;
    let vars = serde_json::to_string(&t.variables)?;
    sqlx::query!("INSERT INTO prompt_templates (id, name, description, category, tags, template, variables, provider_id, is_builtin, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description, category = excluded.category, tags = excluded.tags, template = excluded.template, variables = excluded.variables, provider_id = excluded.provider_id, updated_at = excluded.updated_at",
        id, t.name, t.description, t.category, tags, t.template, vars, t.provider_id, t.is_builtin,
        t.created_at.to_rfc3339(), t.updated_at.to_rfc3339()
    ).execute(pool).await?;
    Ok(id)
}

pub async fn get_memory(pool: &SqlitePool, chat_id: &str) -> Result<Option<ConversationMemory>> {
    let row = sqlx::query!("SELECT id, chat_id, summary, facts, user_preferences, business_context, ai_notes, memory_depth, last_updated FROM conversation_memory WHERE chat_id = ?", chat_id)
        .fetch_optional(pool).await?;
    Ok(row.map(|r| ConversationMemory {
        id: r.id, chat_id: r.chat_id, summary: r.summary,
        facts: r.facts.map(|s| serde_json::from_str(&s).unwrap_or_default()).unwrap_or_default(),
        user_preferences: r.user_preferences.map(|s| serde_json::from_str(&s).unwrap_or_default()).unwrap_or_default(),
        business_context: r.business_context,
        ai_notes: r.ai_notes.map(|s| serde_json::from_str(&s).unwrap_or_default()).unwrap_or_default(),
        memory_depth: r.memory_depth,
        last_updated: r.last_updated.parse().unwrap_or_else(|_| chrono::Utc::now()),
    }))
}

pub async fn save_memory(pool: &SqlitePool, m: &ConversationMemory) -> Result<()> {
    let facts = serde_json::to_string(&m.facts)?;
    let prefs = serde_json::to_string(&m.user_preferences)?;
    let notes = serde_json::to_string(&m.ai_notes)?;
    sqlx::query!("INSERT INTO conversation_memory (id, chat_id, summary, facts, user_preferences, business_context, ai_notes, memory_depth, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(chat_id) DO UPDATE SET summary = excluded.summary, facts = excluded.facts, user_preferences = excluded.user_preferences, business_context = excluded.business_context, ai_notes = excluded.ai_notes, memory_depth = excluded.memory_depth, last_updated = excluded.last_updated",
        m.id, m.chat_id, m.summary, facts, prefs, m.business_context, notes, m.memory_depth, m.last_updated
    ).execute(pool).await?;
    Ok(())
}
