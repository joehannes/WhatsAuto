//! AI service — provider-agnostic chat completion.
//!
//! This module implements the OpenAI-compatible chat API
//! that most providers (Qwen, DeepSeek, Ollama, etc.) support.
//! Provider-specific adapters can be added here without touching commands.

use anyhow::{bail, Result};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use crate::models::ai::{AiMessage, AiProvider, AiProviderKind, AiResponse};
use tracing::debug;

/// Send a chat completion request to an AI provider.
///
/// Uses the OpenAI-compatible `/chat/completions` endpoint
/// which is supported by Qwen, DeepSeek, Ollama, LM Studio, OpenRouter, etc.
/// Claude uses a slightly different API and gets a dedicated adapter.
pub async fn chat(provider: &AiProvider, messages: Vec<AiMessage>) -> Result<AiResponse> {
    match provider.kind {
        AiProviderKind::Claude => chat_claude(provider, messages).await,
        AiProviderKind::Gemini => chat_gemini(provider, messages).await,
        // All others: OpenAI-compatible
        _ => chat_openai_compat(provider, messages).await,
    }
}

// --- OpenAI-compatible adapter ---

#[derive(Serialize)]
struct OaiChatRequest {
    model: String,
    messages: Vec<OaiMessage>,
    #[serde(skip_serializing_if = "Option::is_none")]
    max_tokens: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    temperature: Option<f32>,
}

#[derive(Serialize, Deserialize)]
struct OaiMessage {
    role: String,
    content: String,
}

#[derive(Deserialize)]
struct OaiChatResponse {
    choices: Vec<OaiChoice>,
    usage: Option<OaiUsage>,
    model: Option<String>,
}

#[derive(Deserialize)]
struct OaiChoice {
    message: OaiMessage,
}

#[derive(Deserialize)]
struct OaiUsage {
    total_tokens: Option<i32>,
}

async fn chat_openai_compat(
    provider: &AiProvider,
    messages: Vec<AiMessage>,
) -> Result<AiResponse> {
    let api_key = resolve_api_key(provider)?;

    // Prepend system prompt if configured
    let mut oai_messages: Vec<OaiMessage> = Vec::new();
    if let Some(ref sys) = provider.system_prompt {
        oai_messages.push(OaiMessage {
            role: "system".to_string(),
            content: sys.clone(),
        });
    }
    oai_messages.extend(messages.iter().map(|m| OaiMessage {
        role: format!("{:?}", m.role).to_lowercase(),
        content: m.content.clone(),
    }));

    let req = OaiChatRequest {
        model: provider.model.clone(),
        messages: oai_messages,
        max_tokens: provider.max_tokens,
        temperature: provider.temperature,
    };

    let url = format!("{}/chat/completions", provider.base_url.trim_end_matches('/'));
    debug!("AI request to {}", url);

    let client = Client::new();
    let resp = client
        .post(&url)
        .bearer_auth(&api_key)
        .json(&req)
        .send()
        .await?
        .error_for_status()?
        .json::<OaiChatResponse>()
        .await?;

    let content = resp
        .choices
        .into_iter()
        .next()
        .map(|c| c.message.content)
        .unwrap_or_default();

    Ok(AiResponse {
        content,
        provider_id: provider.id.clone(),
        model: resp.model.unwrap_or_else(|| provider.model.clone()),
        tokens_used: resp.usage.and_then(|u| u.total_tokens),
    })
}

// --- Claude adapter ---

async fn chat_claude(provider: &AiProvider, messages: Vec<AiMessage>) -> Result<AiResponse> {
    let api_key = resolve_api_key(provider)?;

    #[derive(Serialize)]
    struct ClaudeRequest {
        model: String,
        messages: Vec<OaiMessage>,
        #[serde(skip_serializing_if = "Option::is_none")]
        system: Option<String>,
        max_tokens: i32,
    }

    #[derive(Deserialize)]
    struct ClaudeResponse {
        content: Vec<ClaudeContent>,
        model: String,
        usage: Option<ClaudeUsage>,
    }

    #[derive(Deserialize)]
    struct ClaudeContent {
        text: String,
    }

    #[derive(Deserialize)]
    struct ClaudeUsage {
        output_tokens: Option<i32>,
        input_tokens: Option<i32>,
    }

    let oai_messages: Vec<OaiMessage> = messages
        .iter()
        .filter(|m| format!("{:?}", m.role).to_lowercase() != "system")
        .map(|m| OaiMessage {
            role: format!("{:?}", m.role).to_lowercase(),
            content: m.content.clone(),
        })
        .collect();

    let req = ClaudeRequest {
        model: provider.model.clone(),
        messages: oai_messages,
        system: provider.system_prompt.clone(),
        max_tokens: provider.max_tokens.unwrap_or(4096),
    };

    let url = format!("{}/messages", provider.base_url.trim_end_matches('/'));
    let client = Client::new();
    let resp = client
        .post(&url)
        .header("x-api-key", &api_key)
        .header("anthropic-version", "2023-06-01")
        .json(&req)
        .send()
        .await?
        .error_for_status()?
        .json::<ClaudeResponse>()
        .await?;

    let content = resp.content.into_iter().next().map(|c| c.text).unwrap_or_default();
    let tokens = resp.usage.and_then(|u| {
        Some((u.input_tokens.unwrap_or(0) + u.output_tokens.unwrap_or(0)) as i32)
    });

    Ok(AiResponse {
        content,
        provider_id: provider.id.clone(),
        model: resp.model,
        tokens_used: tokens,
    })
}

// --- Gemini stub (REST v1beta) ---

async fn chat_gemini(provider: &AiProvider, messages: Vec<AiMessage>) -> Result<AiResponse> {
    // Gemini uses a distinct API format; stub returns helpful error until implemented
    bail!("Gemini provider not yet fully implemented. Use the REST v1beta adapter.");
}

// --- Helpers ---

fn resolve_api_key(provider: &AiProvider) -> Result<String> {
    let key_ref = provider
        .api_key_ref
        .as_deref()
        .unwrap_or_default();

    if key_ref.is_empty() {
        // Providers like Ollama don't need an API key
        return Ok(String::new());
    }

    let entry = keyring::Entry::new("whatsauto", key_ref)?;
    let key = entry.get_password().unwrap_or_default();
    Ok(key)
}
