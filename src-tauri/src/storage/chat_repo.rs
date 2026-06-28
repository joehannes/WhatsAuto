use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::Chat;

pub async fn list_chats(pool: &SqlitePool) -> Result<Vec<Chat>> {
    let rows = sqlx::query!("SELECT id, jid, name, avatar_url, last_message_preview, last_message_at, unread_count, is_archived, is_pinned, is_group, labels, conversation_mode, ai_provider_id, created_at, updated_at FROM chats ORDER BY last_message_at DESC NULLS LAST")
        .fetch_all(pool).await?;
    let chats: Vec<Chat> = rows.into_iter().map(|r| Chat {
        id: crate::models::ChatId(r.id),
        jid: r.jid,
        name: r.name,
        avatar_url: r.avatar_url,
        last_message_preview: r.last_message_preview,
        last_message_at: r.last_message_at,
        unread_count: r.unread_count,
        is_archived: r.is_archived != 0,
        is_pinned: r.is_pinned != 0,
        is_group: r.is_group != 0,
        labels: r.labels.map(|s| serde_json::from_str(&s).unwrap_or_default()).unwrap_or_default(),
        conversation_mode: match r.conversation_mode.as_deref() {
            Some("assisted") => crate::models::ConversationMode::Assisted,
            Some("autonomous") => crate::models::ConversationMode::Autonomous,
            _ => crate::models::ConversationMode::Human,
        },
        ai_provider_id: r.ai_provider_id,
        created_at: r.created_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
        updated_at: r.updated_at.parse().unwrap_or_else(|_| chrono::Utc::now()),
    }).collect();
    Ok(chats)
}

pub async fn upsert_chat(pool: &SqlitePool, chat: &Chat) -> Result<()> {
    let labels = serde_json::to_string(&chat.labels)?;
    let mode = match chat.conversation_mode {
        crate::models::ConversationMode::Assisted => "assisted",
        crate::models::ConversationMode::Autonomous => "autonomous",
        crate::models::ConversationMode::Human => "human",
    };
    let last_at = chat.last_message_at.map(|dt| dt.to_rfc3339());
    sqlx::query!("INSERT INTO chats (id, jid, name, avatar_url, last_message_preview, last_message_at, unread_count, is_archived, is_pinned, is_group, labels, conversation_mode, ai_provider_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(jid) DO UPDATE SET name = excluded.name, avatar_url = excluded.avatar_url, last_message_preview = excluded.last_message_preview, last_message_at = excluded.last_message_at, unread_count = excluded.unread_count, is_archived = excluded.is_archived, is_pinned = excluded.is_pinned, labels = excluded.labels, conversation_mode = excluded.conversation_mode, ai_provider_id = excluded.ai_provider_id, updated_at = excluded.updated_at",
        chat.id.0, chat.jid, chat.name, chat.avatar_url, chat.last_message_preview, last_at, chat.unread_count, chat.is_archived, chat.is_pinned, chat.is_group, labels, mode, chat.ai_provider_id,
        chat.created_at.to_rfc3339(), chat.updated_at.to_rfc3339()
    ).execute(pool).await?;
    Ok(())
}
