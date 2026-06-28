//! Message repository.

use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::message::{Message, MessageType};
use chrono::{DateTime, Utc};

/// Insert a new message (skip on duplicate id).
pub async fn insert_message(pool: &SqlitePool, msg: &Message) -> Result<()> {
    let type_str = match msg.message_type {
        MessageType::Text => "text",
        MessageType::Image => "image",
        MessageType::Video => "video",
        MessageType::Audio => "audio",
        MessageType::Document => "document",
        MessageType::Sticker => "sticker",
        MessageType::Location => "location",
        MessageType::Contact => "contact",
        MessageType::Reaction => "reaction",
        MessageType::System => "system",
    };

    sqlx::query!(
        r#"
        INSERT OR IGNORE INTO messages
            (id, chat_id, from_me, author, body, message_type, media_url, media_mime,
             timestamp, is_read, is_starred, reaction, ai_provider)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        "#,
        msg.id,
        msg.chat_id,
        msg.from_me,
        msg.author,
        msg.body,
        type_str,
        msg.media_url,
        msg.media_mime,
        msg.timestamp,
        msg.is_read,
        msg.is_starred,
        msg.reaction,
        msg.ai_provider
    )
    .execute(pool)
    .await?;

    Ok(())
}

/// Fetch messages for a chat, newest first with optional cursor pagination.
pub async fn get_messages(
    pool: &SqlitePool,
    chat_id: &str,
    limit: i64,
    before: Option<DateTime<Utc>>,
) -> Result<Vec<Message>> {
    let before_str = before.map(|dt| dt.to_rfc3339());

    // Two queries to keep sqlx macro happy with optional WHERE clause
    let rows = if let Some(ref before_val) = before_str {
        sqlx::query!(
            r#"
            SELECT id, chat_id, from_me, author, body, message_type, media_url, media_mime,
                   timestamp, is_read, is_starred, reaction, ai_provider
            FROM messages
            WHERE chat_id = ? AND timestamp < ?
            ORDER BY timestamp DESC
            LIMIT ?
            "#,
            chat_id,
            before_val,
            limit
        )
        .fetch_all(pool)
        .await?
    } else {
        sqlx::query!(
            r#"
            SELECT id, chat_id, from_me, author, body, message_type, media_url, media_mime,
                   timestamp, is_read, is_starred, reaction, ai_provider
            FROM messages
            WHERE chat_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            "#,
            chat_id,
            limit
        )
        .fetch_all(pool)
        .await?
    };

    let messages = rows
        .into_iter()
        .map(|r| {
            let ts = r
                .timestamp
                .parse::<DateTime<Utc>>()
                .unwrap_or_else(|_| Utc::now());

            Message {
                id: r.id,
                chat_id: r.chat_id,
                from_me: r.from_me != 0,
                author: r.author,
                body: r.body,
                message_type: parse_message_type(&r.message_type),
                media_url: r.media_url,
                media_mime: r.media_mime,
                timestamp: ts,
                is_read: r.is_read != 0,
                is_starred: r.is_starred != 0,
                reaction: r.reaction,
                ai_provider: r.ai_provider,
            }
        })
        .collect();

    Ok(messages)
}

fn parse_message_type(s: &str) -> MessageType {
    match s {
        "image" => MessageType::Image,
        "video" => MessageType::Video,
        "audio" => MessageType::Audio,
        "document" => MessageType::Document,
        "sticker" => MessageType::Sticker,
        "location" => MessageType::Location,
        "contact" => MessageType::Contact,
        "reaction" => MessageType::Reaction,
        "system" => MessageType::System,
        _ => MessageType::Text,
    }
}
