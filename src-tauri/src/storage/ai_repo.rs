//! AI provider configuration repository.

use anyhow::Result;
use sqlx::SqlitePool;
use uuid::Uuid;
use crate::models::ai::{AiProvider, AiProviderKind};

pub async fn list_providers(pool: &SqlitePool) -> Result<Vec<AiProvider>> {
    let rows = sqlx::query!(
        "SELECT id, kind, name, base_url, model, api_key_ref, is_default, enabled, system_prompt, max_tokens, temperature FROM ai_providers ORDER BY name"
    )
    .fetch_all(pool)
    .await?;

    Ok(rows
        .into_iter()
        .map(|r| AiProvider {
            id: r.id,
            kind: parse_kind(&r.kind),
            name: r.name,
            base_url: r.base_url,
            model: r.model,
            api_key_ref: r.api_key_ref,
            is_default: r.is_default != 0,
            enabled: r.enabled != 0,
            system_prompt: r.system_prompt,
            max_tokens: r.max_tokens.map(|v| v as i32),
            temperature: r.temperature.map(|v| v as f32),
        })
        .collect())
}

pub async fn save_provider(pool: &SqlitePool, p: &AiProvider) -> Result<String> {
    let id = if p.id.is_empty() {
        Uuid::new_v4().to_string()
    } else {
        p.id.clone()
    };
    let kind_str = format!("{:?}", p.kind).to_lowercase();
    sqlx::query!(
        r#"
        INSERT INTO ai_providers (id, kind, name, base_url, model, api_key_ref, is_default, enabled, system_prompt, max_tokens, temperature)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            kind = excluded.kind,
            name = excluded.name,
            base_url = excluded.base_url,
            model = excluded.model,
            api_key_ref = excluded.api_key_ref,
            is_default = excluded.is_default,
            enabled = excluded.enabled,
            system_prompt = excluded.system_prompt,
            max_tokens = excluded.max_tokens,
            temperature = excluded.temperature
        "#,
        id, kind_str, p.name, p.base_url, p.model, p.api_key_ref,
        p.is_default, p.enabled, p.system_prompt, p.max_tokens, p.temperature
    )
    .execute(pool)
    .await?;
    Ok(id)
}

pub async fn delete_provider(pool: &SqlitePool, id: &str) -> Result<()> {
    sqlx::query!("DELETE FROM ai_providers WHERE id = ?", id)
        .execute(pool)
        .await?;
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
        _ => AiProviderKind::Qwen,
    }
}
