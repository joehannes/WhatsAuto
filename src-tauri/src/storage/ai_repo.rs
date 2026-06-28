use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::ai::{AiProvider, AiProviderKind};

pub async fn list_providers(pool: &SqlitePool) -> Result<Vec<AiProvider>> {
    let rows = sqlx::query!("SELECT id, kind, name, base_url, model, api_key_ref, is_default, enabled, system_prompt, max_tokens, temperature, top_p, streaming_enabled, request_timeout, retry_count, created_at, updated_at FROM ai_providers ORDER BY name")
        .fetch_all(pool).await?;
    Ok(rows.into_iter().map(|r| AiProvider {
        id: r.id, kind: parse_kind(&r.kind), name: r.name, base_url: r.base_url, model: r.model,
        api_key_ref: r.api_key_ref, is_default: r.is_default != 0, enabled: r.enabled != 0,
        system_prompt: r.system_prompt, max_tokens: r.max_tokens, temperature: r.temperature,
        top_p: r.top_p, streaming_enabled: r.streaming_enabled != 0,
        request_timeout: r.request_timeout, retry_count: r.retry_count,
        created_at: r.created_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
        updated_at: r.updated_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
    }).collect())
}

pub async fn save_provider(pool: &SqlitePool, p: &AiProvider) -> Result<String> {
    let id = if p.id.is_empty() { uuid::Uuid::new_v4().to_string() } else { p.id.clone() };
    let kind_str = format!("{:?}", p.kind).to_lowercase();
    sqlx::query!("INSERT INTO ai_providers (id, kind, name, base_url, model, api_key_ref, is_default, enabled, system_prompt, max_tokens, temperature, top_p, streaming_enabled, request_timeout, retry_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET kind = excluded.kind, name = excluded.name, base_url = excluded.base_url, model = excluded.model, api_key_ref = excluded.api_key_ref, is_default = excluded.is_default, enabled = excluded.enabled, system_prompt = excluded.system_prompt, max_tokens = excluded.max_tokens, temperature = excluded.temperature, top_p = excluded.top_p, streaming_enabled = excluded.streaming_enabled, request_timeout = excluded.request_timeout, retry_count = excluded.retry_count, updated_at = excluded.updated_at",
        id, kind_str, p.name, p.base_url, p.model, p.api_key_ref, p.is_default, p.enabled,
        p.system_prompt, p.max_tokens, p.temperature, p.top_p, p.streaming_enabled,
        p.request_timeout, p.retry_count, p.created_at.to_rfc3339(), p.updated_at.to_rfc3339()
    ).execute(pool).await?;
    Ok(id)
}

pub async fn delete_provider(pool: &SqlitePool, id: &str) -> Result<()> {
    sqlx::query!("DELETE FROM ai_providers WHERE id = ?", id).execute(pool).await?;
    Ok(())
}

fn parse_kind(s: &str) -> AiProviderKind {
    match s {
        "openai" => AiProviderKind::OpenAi,
        "claude" => AiProviderKind::Claude,
        "gemini" => AiProviderKind::Gemini,
        "deepseek" => AiProviderKind::DeepSeek,
        "ollama" => AiProviderKind::Ollama,
        "lmstudio" => AiProviderKind::LmStudio,
        "openrouter" => AiProviderKind::OpenRouter,
        "custom" => AiProviderKind::Custom,
        _ => AiProviderKind::Qwen,
    }
}
