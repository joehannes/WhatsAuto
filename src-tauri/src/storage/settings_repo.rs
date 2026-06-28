use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::settings::Settings;

pub async fn get_settings(pool: &SqlitePool) -> Result<Settings> {
    let row = sqlx::query!("SELECT theme, language, notifications_enabled, sound_enabled, auto_reply_enabled, auto_reply_provider_id, auto_reply_delay_seconds, default_ai_provider_id, business_name, business_phone, business_email, memory_enabled, memory_depth, translation_enabled, default_translation_style, voice_enabled, voice_stt_provider, voice_tts_provider, plugins_enabled, scheduler_enabled, business_hours_start, business_hours_end, timezone FROM settings WHERE id = 1")
        .fetch_optional(pool).await?;
    Ok(match row {
        Some(r) => Settings {
            theme: match r.theme.as_deref() { Some("light") => crate::models::settings::Theme::Light, Some("dark") => crate::models::settings::Theme::Dark, _ => crate::models::settings::Theme::System },
            language: r.language, notifications_enabled: r.notifications_enabled != 0,
            sound_enabled: r.sound_enabled != 0, auto_reply_enabled: r.auto_reply_enabled != 0,
            auto_reply_provider_id: r.auto_reply_provider_id,
            auto_reply_delay_seconds: r.auto_reply_delay_seconds,
            default_ai_provider_id: r.default_ai_provider_id,
            business_name: r.business_name, business_phone: r.business_phone,
            business_email: r.business_email, memory_enabled: r.memory_enabled != 0,
            memory_depth: r.memory_depth, translation_enabled: r.translation_enabled != 0,
            default_translation_style: r.default_translation_style,
            voice_enabled: r.voice_enabled != 0,
            voice_stt_provider: r.voice_stt_provider,
            voice_tts_provider: r.voice_tts_provider,
            plugins_enabled: r.plugins_enabled != 0,
            scheduler_enabled: r.scheduler_enabled != 0,
            business_hours_start: r.business_hours_start,
            business_hours_end: r.business_hours_end,
            timezone: r.timezone,
        },
        None => Settings::default(),
    })
}

pub async fn save_settings(pool: &SqlitePool, s: &Settings) -> Result<()> {
    let theme = match s.theme { crate::models::settings::Theme::Light => "light", crate::models::settings::Theme::Dark => "dark", _ => "system" };
    sqlx::query!("INSERT INTO settings (id, theme, language, notifications_enabled, sound_enabled, auto_reply_enabled, auto_reply_provider_id, auto_reply_delay_seconds, default_ai_provider_id, business_name, business_phone, business_email, memory_enabled, memory_depth, translation_enabled, default_translation_style, voice_enabled, voice_stt_provider, voice_tts_provider, plugins_enabled, scheduler_enabled, business_hours_start, business_hours_end, timezone) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET theme = excluded.theme, language = excluded.language, notifications_enabled = excluded.notifications_enabled, sound_enabled = excluded.sound_enabled, auto_reply_enabled = excluded.auto_reply_enabled, auto_reply_provider_id = excluded.auto_reply_provider_id, auto_reply_delay_seconds = excluded.auto_reply_delay_seconds, default_ai_provider_id = excluded.default_ai_provider_id, business_name = excluded.business_name, business_phone = excluded.business_phone, business_email = excluded.business_email, memory_enabled = excluded.memory_enabled, memory_depth = excluded.memory_depth, translation_enabled = excluded.translation_enabled, default_translation_style = excluded.default_translation_style, voice_enabled = excluded.voice_enabled, voice_stt_provider = excluded.voice_stt_provider, voice_tts_provider = excluded.voice_tts_provider, plugins_enabled = excluded.plugins_enabled, scheduler_enabled = excluded.scheduler_enabled, business_hours_start = excluded.business_hours_start, business_hours_end = excluded.business_hours_end, timezone = excluded.timezone",
        theme, s.language, s.notifications_enabled, s.sound_enabled, s.auto_reply_enabled, s.auto_reply_provider_id, s.auto_reply_delay_seconds, s.default_ai_provider_id, s.business_name, s.business_phone, s.business_email, s.memory_enabled, s.memory_depth, s.translation_enabled, s.default_translation_style, s.voice_enabled, s.voice_stt_provider, s.voice_tts_provider, s.plugins_enabled, s.scheduler_enabled, s.business_hours_start, s.business_hours_end, s.timezone
    ).execute(pool).await?;
    Ok(())
}
