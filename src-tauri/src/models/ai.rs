use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
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

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AiProvider {
    pub id: String,
    pub kind: AiProviderKind,
    pub name: String,
    pub base_url: String,
    pub model: String,
    #[serde(skip_serializing)]
    pub api_key_ref: Option<String>,
    pub is_default: bool,
    pub enabled: bool,
    pub system_prompt: Option<String>,
    pub max_tokens: Option<i32>,
    pub temperature: Option<f32>,
    pub top_p: Option<f32>,
    pub streaming_enabled: bool,
    pub request_timeout: Option<i32>,
    pub retry_count: Option<i32>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

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
    Tool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AiResponse {
    pub content: String,
    pub provider_id: String,
    pub model: String,
    pub tokens_used: Option<i32>,
    pub tokens_input: Option<i32>,
    pub tokens_output: Option<i32>,
    pub finish_reason: Option<String>,
    pub response_time_ms: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PromptTemplate {
    pub id: String,
    pub name: String,
    pub description: Option<String>,
    pub category: String,
    pub tags: Vec<String>,
    pub template: String,
    pub variables: Vec<String>,
    pub provider_id: Option<String>,
    pub is_builtin: bool,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConversationMemory {
    pub id: String,
    pub chat_id: String,
    pub summary: Option<String>,
    pub facts: Vec<String>,
    pub user_preferences: Vec<String>,
    pub business_context: Option<String>,
    pub ai_notes: Vec<String>,
    pub memory_depth: i32,
    pub last_updated: DateTime<Utc>,
}
