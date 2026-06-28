//! Message domain types.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// A single message in a conversation.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Message {
    pub id: String,
    pub chat_id: String,
    pub from_me: bool,
    /// Sender JID
    pub author: String,
    pub body: Option<String>,
    pub message_type: MessageType,
    pub media_url: Option<String>,
    pub media_mime: Option<String>,
    pub timestamp: DateTime<Utc>,
    pub is_read: bool,
    pub is_starred: bool,
    pub reaction: Option<String>,
    /// For AI-generated messages: which provider sent this
    pub ai_provider: Option<String>,
}

/// The type of content in a message.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, sqlx::Type)]
#[serde(rename_all = "snake_case")]
#[sqlx(type_name = "TEXT", rename_all = "snake_case")]
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
}

/// Delivery / read receipt status.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum MessageStatus {
    Sent,
    Delivered,
    Read,
    Failed,
}

/// Typing indicator event.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TypingEvent {
    pub chat_id: String,
    pub is_typing: bool,
}
