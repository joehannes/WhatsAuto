//! Application settings types.

use serde::{Deserialize, Serialize};

/// Top-level application settings.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    pub theme: Theme,
    pub language: String,
    pub notifications_enabled: bool,
    pub sound_enabled: bool,
    pub auto_reply_enabled: bool,
    pub default_ai_provider_id: Option<String>,
    pub business_name: Option<String>,
    pub business_phone: Option<String>,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            theme: Theme::System,
            language: "en".to_string(),
            notifications_enabled: true,
            sound_enabled: true,
            auto_reply_enabled: false,
            default_ai_provider_id: None,
            business_name: None,
            business_phone: None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Theme {
    Light,
    Dark,
    System,
}
