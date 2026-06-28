use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    pub theme: Theme,
    pub language: String,
    pub notifications_enabled: bool,
    pub sound_enabled: bool,
    pub auto_reply_enabled: bool,
    pub auto_reply_provider_id: Option<String>,
    pub auto_reply_delay_seconds: Option<i32>,
    pub default_ai_provider_id: Option<String>,
    pub business_name: Option<String>,
    pub business_phone: Option<String>,
    pub business_email: Option<String>,
    pub memory_enabled: bool,
    pub memory_depth: i32,
    pub translation_enabled: bool,
    pub default_translation_style: Option<String>,
    pub voice_enabled: bool,
    pub voice_stt_provider: Option<String>,
    pub voice_tts_provider: Option<String>,
    pub plugins_enabled: bool,
    pub scheduler_enabled: bool,
    pub business_hours_start: Option<String>,
    pub business_hours_end: Option<String>,
    pub timezone: Option<String>,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            theme: Theme::System,
            language: "en".to_string(),
            notifications_enabled: true,
            sound_enabled: true,
            auto_reply_enabled: false,
            auto_reply_provider_id: None,
            auto_reply_delay_seconds: Some(5),
            default_ai_provider_id: None,
            business_name: None,
            business_phone: None,
            business_email: None,
            memory_enabled: false,
            memory_depth: 10,
            translation_enabled: false,
            default_translation_style: Some("business".to_string()),
            voice_enabled: false,
            voice_stt_provider: None,
            voice_tts_provider: None,
            plugins_enabled: false,
            scheduler_enabled: false,
            business_hours_start: Some("09:00".to_string()),
            business_hours_end: Some("18:00".to_string()),
            timezone: Some("UTC".to_string()),
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
