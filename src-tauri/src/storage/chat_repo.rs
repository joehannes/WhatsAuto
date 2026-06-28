//! Chat repository — CRUD for chats.

use anyhow::Result;
use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use crate::models::chat::{Chat, ChatId, ConversationMode};

/// Fetch all chats ordered by last message timestamp.
pub async fn list_chats(pool: &SqlitePool) -> Result<Vec<Chat>> {
    let rows = sqlx::query!(
        r#"
        SELECT
            id, jid, name, avatar_url,
            last_message_preview, last_message_at,
            unread_count, is_archived, is_group,
            labels, conversation_mode
        FROM chats
        ORDER BY last_message_at DESC NULLS LAST
        "#
    )
    .fetch_all(pool)
    .await?;

    let chats = rows
        .into_iter()
        .map(|r| Chat {
            id: ChatId(r.id),
            jid: r.jid,
            name: r.name,
            avatar_url: r.avatar_url,
            last_message_preview: r.last_message_preview,
            last_message_at: r.last_message_at.and_then(|s| s.parse::<DateTime<Utc>>().ok()),
            unread_count: r.unread_count,
            is_archived: r.is_archived != 0,
            is_group: r.is_group != 0,
            labels: r.labels
                .map(|s| serde_json::from_str(&s).unwrap_or_default())
                .unwrap_or_default(),
            conversation_mode: match r.conversation_mode.as_deref() {
                Some("ai") => ConversationMode::Ai,
                Some("hybrid") => ConversationMode::Hybrid,
                _ => ConversationMode::Human,
            },
        })
        .collect();

    Ok(chats)
}

/// Upsert a chat (insert or update on jid conflict).
pub async fn upsert_chat(pool: &SqlitePool, chat: &Chat) -> Result<()> {
    let labels = serde_json::to_string(&chat.labels)?;
    let mode = match chat.conversation_mode {
        ConversationMode::Ai => "ai",
        ConversationMode::Hybrid => "hybrid",
        ConversationMode::Human => "human",
    };
    let last_msg_at = chat.last_message_at.map(|dt| dt.to_rfc3339());

    sqlx::query!(
        r#"
        INSERT INTO chats (id, jid, name, avatar_url, last_message_preview, last_message_at,
                           unread_count, is_archived, is_group, labels, conversation_mode)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(jid) DO UPDATE SET
            name = excluded.name,
            avatar_url = excluded.avatar_url,
            last_message_preview = excluded.last_message_preview,
            last_message_at = excluded.last_message_at,
            unread_count = excluded.unread_count,
            is_archived = excluded.is_archived,
            labels = excluded.labels,
            conversation_mode = excluded.conversation_mode
        "#,
        chat.id.0,
        chat.jid,
        chat.name,
        chat.avatar_url,
        chat.last_message_preview,
        last_msg_at,
        chat.unread_count,
        chat.is_archived,
        chat.is_group,
        labels,
        mode
    )
    .execute(pool)
    .await?;

    Ok(())
}

/// Update the conversation ownership mode.
pub async fn set_conversation_mode(
    pool: &SqlitePool,
    chat_id: &str,
    mode: &ConversationMode,
) -> Result<()> {
    let mode_str = match mode {
        ConversationMode::Ai => "ai",
        ConversationMode::Hybrid => "hybrid",
        ConversationMode::Human => "human",
    };
    sqlx::query!(
        "UPDATE chats SET conversation_mode = ? WHERE id = ?",
        mode_str,
        chat_id
    )
    .execute(pool)
    .await?;
    Ok(())
}

/// Archive or unarchive a chat.
pub async fn set_archived(pool: &SqlitePool, chat_id: &str, archived: bool) -> Result<()> {
    let v: i64 = if archived { 1 } else { 0 };
    sqlx::query!("UPDATE chats SET is_archived = ? WHERE id = ?", v, chat_id)
        .execute(pool)
        .await?;
    Ok(())
}
