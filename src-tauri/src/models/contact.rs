//! Contact domain types.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// A WhatsApp contact or business entity.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Contact {
    pub id: String,
    /// WhatsApp JID
    pub jid: String,
    pub name: String,
    pub push_name: Option<String>,
    pub phone_number: Option<String>,
    pub avatar_url: Option<String>,
    pub business_name: Option<String>,
    pub notes: Option<String>,
    pub tags: Vec<String>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}
