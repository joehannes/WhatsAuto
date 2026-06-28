//! AI provider and interaction types.

use serde::{Deserialize, Serialize};

/// Supported AI providers.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, sqlx::Type)]
#[serde(rename_all = "snake_case")]
#[sqlx(type_name = "TEXT", rename_all = "snake_case")]
pub enum AiProviderKind {
    Qwen,
    OpenAi,
    Claude,
    Gemini,
    DeepSeek,
    Ollama,
    LmStudio,
    OpenRouter,
    Custom,
}

impl AiProviderKind {
    /// Returns the default base URL for this provider.
    pub fn default_base_url(&self) -> &str {
        match self {
            Self::Qwen => "https://dashscope.aliyuncs.com/compatible-mode/v1",
            Self::OpenAi => "https://api.openai.com/v1",
            Self::Claude => "https://api.anthropic.com/v1",
            Self::Gemini => "https://generativelanguage.googleapis.com/v1beta",
            Self::DeepSeek => "https://api.deepseek.com/v1",
            Self::Ollama => "http://localhost:11434/v1",
            Self::LmStudio => "http://localhost:1234/v1",
            Self::OpenRouter => "https://openrouter.ai/api/v1",
            Self::Custom => "",
        }
    }
}

/// Configuration for an AI provider instance.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AiProvider {
    pub id: String,
    pub kind: AiProviderKind,
    pub name: String,
    pub base_url: String,
    pub model: String,
    /// API key stored via OS keychain; not serialised in transit
    #[serde(skip_serializing)]
    pub api_key_ref: Option<String>,
    pub is_default: bool,
    pub enabled: bool,
    pub system_prompt: Option<String>,
    /// Max tokens per response
    pub max_tokens: Option<i32>,
    pub temperature: Option<f32>,
}

/// A single turn in an AI conversation.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AiMessage {
    pub role: AiRole,
    pub content: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum AiRole {
    System,
    User,
    Assistant,
}

/// Response from the AI service.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AiResponse {
    pub content: String,
    pub provider_id: String,
    pub model: String,
    pub tokens_used: Option<i32>,
}

/// A reusable prompt template.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PromptTemplate {
    pub id: String,
    pub name: String,
    pub description: Option<String>,
    pub template: String,
    pub variables: Vec<String>,
    pub provider_id: Option<String>,
}
