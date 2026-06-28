use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct ChatId(pub String);

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Chat {
    pub id: ChatId,
    pub jid: String,
    pub name: String,
    pub avatar_url: Option<String>,
    pub last_message_preview: Option<String>,
    pub last_message_at: Option<DateTime<Utc>>,
    pub unread_count: i32,
    pub is_archived: bool,
    pub is_pinned: bool,
    pub is_group: bool,
    pub labels: Vec<String>,
    pub conversation_mode: ConversationMode,
    pub ai_provider_id: Option<String>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum ConversationMode {
    #[default]
    Human,
    Assisted,
    Autonomous,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Message {
    pub id: String,
    pub chat_id: String,
    pub from_me: bool,
    pub author: String,
    pub body: Option<String>,
    pub message_type: MessageType,
    pub media_url: Option<String>,
    pub media_mime: Option<String>,
    pub media_file_name: Option<String>,
    pub media_size: Option<i64>,
    pub timestamp: DateTime<Utc>,
    pub is_read: bool,
    pub is_starred: bool,
    pub is_pinned: bool,
    pub reaction: Option<String>,
    pub quoted_message_id: Option<String>,
    pub ai_provider_id: Option<String>,
    pub ai_confidence: Option<f32>,
    pub schedule_id: Option<String>,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum MessageType {
    Text,
    Image,
    Video,
    Audio,
    Document,
    Sticker,
    Location,
    Contact,
    Reaction,
    System,
    Template,
}

impl Default for MessageType {
    fn default() -> Self { Self::Text }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Contact {
    pub id: String,
    pub jid: String,
    pub name: String,
    pub push_name: Option<String>,
    pub phone_number: Option<String>,
    pub avatar_url: Option<String>,
    pub business_name: Option<String>,
    pub business_category: Option<String>,
    pub notes: Option<String>,
    pub tags: Vec<String>,
    pub language: Option<String>,
    pub is_business: bool,
    pub is_verified: bool,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}
