//! Chat and conversation domain types.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Unique identifier for a WhatsApp chat (JID-based or local UUID).
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct ChatId(pub String);

/// High-level chat summary shown in the chat list.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Chat {
    pub id: ChatId,
    /// WhatsApp JID (e.g. 491701234567@c.us or group@g.us)
    pub jid: String,
    pub name: String,
    pub avatar_url: Option<String>,
    pub last_message_preview: Option<String>,
    pub last_message_at: Option<DateTime<Utc>>,
    pub unread_count: i32,
    pub is_archived: bool,
    pub is_group: bool,
    /// Application-side labels (not WhatsApp labels)
    pub labels: Vec<String>,
    /// Which agent currently owns this conversation
    pub conversation_mode: ConversationMode,
}

/// Ownership mode for a conversation.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, sqlx::Type)]
#[serde(rename_all = "snake_case")]
#[sqlx(type_name = "TEXT", rename_all = "snake_case")]
pub enum ConversationMode {
    Human,
    Ai,
    Hybrid,
}

impl Default for ConversationMode {
    fn default() -> Self {
        Self::Human
    }
}
