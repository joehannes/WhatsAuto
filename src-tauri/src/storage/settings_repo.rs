//! Settings repository.

use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::settings::{Settings, Theme};

pub async fn get_settings(pool: &SqlitePool) -> Result<Settings> {
    let row = sqlx::query!(
        "SELECT theme, language, notifications_enabled, sound_enabled, auto_reply_enabled, default_ai_provider_id, business_name, business_phone FROM settings WHERE id = 1"
    )
    .fetch_optional(pool)
    .await?;

    Ok(match row {
        Some(r) => Settings {
            theme: match r.theme.as_deref() {
                Some("light") => Theme::Light,
                Some("dark") => Theme::Dark,
                _ => Theme::System,
            },
            language: r.language,
            notifications_enabled: r.notifications_enabled != 0,
            sound_enabled: r.sound_enabled != 0,
            auto_reply_enabled: r.auto_reply_enabled != 0,
            default_ai_provider_id: r.default_ai_provider_id,
            business_name: r.business_name,
            business_phone: r.business_phone,
        },
        None => Settings::default(),
    })
}

pub async fn save_settings(pool: &SqlitePool, s: &Settings) -> Result<()> {
    let theme_str = match s.theme {
        Theme::Light => "light",
        Theme::Dark => "dark",
        Theme::System => "system",
    };
    sqlx::query!(
        r#"
        INSERT INTO settings (id, theme, language, notifications_enabled, sound_enabled,
                              auto_reply_enabled, default_ai_provider_id, business_name, business_phone)
        VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            theme = excluded.theme,
            language = excluded.language,
            notifications_enabled = excluded.notifications_enabled,
            sound_enabled = excluded.sound_enabled,
            auto_reply_enabled = excluded.auto_reply_enabled,
            default_ai_provider_id = excluded.default_ai_provider_id,
            business_name = excluded.business_name,
            business_phone = excluded.business_phone
        "#,
        theme_str, s.language, s.notifications_enabled, s.sound_enabled,
        s.auto_reply_enabled, s.default_ai_provider_id, s.business_name, s.business_phone
    )
    .execute(pool)
    .await?;
    Ok(())
}
