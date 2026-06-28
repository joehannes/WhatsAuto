use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::Message;

pub async fn insert_message(pool: &SqlitePool, msg: &Message) -> Result<()> {
    let type_str = match msg.message_type {
        crate::models::MessageType::Text => "text",
        crate::models::MessageType::Image => "image",
        crate::models::MessageType::Video => "video",
        crate::models::MessageType::Audio => "audio",
        crate::models::MessageType::Document => "document",
        crate::models::MessageType::Sticker => "sticker",
        crate::models::MessageType::Location => "location",
        crate::models::MessageType::Contact => "contact",
        crate::models::MessageType::Reaction => "reaction",
        crate::models::MessageType::System => "system",
        crate::models::MessageType::Template => "template",
    };
    sqlx::query!("INSERT OR IGNORE INTO messages (id, chat_id, from_me, author, body, message_type, media_url, media_mime, media_file_name, media_size, timestamp, is_read, is_starred, is_pinned, reaction, quoted_message_id, ai_provider_id, ai_confidence, schedule_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        msg.id, msg.chat_id, msg.from_me, msg.author, msg.body, type_str, msg.media_url, msg.media_mime, msg.media_file_name, msg.media_size, msg.timestamp, msg.is_read, msg.is_starred, msg.is_pinned, msg.reaction, msg.quoted_message_id, msg.ai_provider_id, msg.ai_confidence, msg.schedule_id, msg.created_at
    ).execute(pool).await?;
    Ok(())
}

pub async fn get_messages(pool: &SqlitePool, chat_id: &str, limit: i64) -> Result<Vec<Message>> {
    let rows = sqlx::query!("SELECT id, chat_id, from_me, author, body, message_type, media_url, media_mime, media_file_name, media_size, timestamp, is_read, is_starred, is_pinned, reaction, quoted_message_id, ai_provider_id, ai_confidence, schedule_id, created_at FROM messages WHERE chat_id = ? ORDER BY timestamp DESC LIMIT ?", chat_id, limit)
        .fetch_all(pool).await?;
    Ok(rows.into_iter().map(|r| Message {
        id: r.id, chat_id: r.chat_id, from_me: r.from_me != 0, author: r.author,
        body: r.body, message_type: parse_type(&r.message_type),
        media_url: r.media_url, media_mime: r.media_mime, media_file_name: r.media_file_name,
        media_size: r.media_size, timestamp: r.timestamp.parse().unwrap_or_else(|_| chrono::Utc::now()),
        is_read: r.is_read != 0, is_starred: r.is_starred != 0, is_pinned: r.is_pinned != 0,
        reaction: r.reaction, quoted_message_id: r.quoted_message_id,
        ai_provider_id: r.ai_provider_id, ai_confidence: r.ai_confidence,
        schedule_id: r.schedule_id, created_at: r.created_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
    }).collect())
}

fn parse_type(s: &str) -> crate::models::MessageType {
    match s {
        "image" => crate::models::MessageType::Image,
        "video" => crate::models::MessageType::Video,
        "audio" => crate::models::MessageType::Audio,
        "document" => crate::models::MessageType::Document,
        "sticker" => crate::models::MessageType::Sticker,
        "location" => crate::models::MessageType::Location,
        "contact" => crate::models::MessageType::Contact,
        "reaction" => crate::models::MessageType::Reaction,
        "system" => crate::models::MessageType::System,
        "template" => crate::models::MessageType::Template,
        _ => crate::models::MessageType::Text,
    }
}
