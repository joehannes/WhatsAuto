use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum SidecarEvent {
    QrCode { qr: String },
    Authenticated,
    Disconnected { reason: Option<String> },
    MessageReceived { message: serde_json::Value },
    MessageAck { id: String, ack: i32 },
    MessageReaction { id: String, emoji: String, sender_id: String },
    TypingStart { jid: String },
    TypingStop { jid: String },
    PresenceUpdate { jid: String, status: String },
    ContactsSync { contacts: Vec<serde_json::Value> },
    ChatsSync { chats: Vec<serde_json::Value> },
    ChatUpdate { id: String, pinned: bool, archived: bool, muted: bool },
}
