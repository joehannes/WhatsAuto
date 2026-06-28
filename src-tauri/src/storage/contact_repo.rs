//! Contact repository.

use anyhow::Result;
use sqlx::SqlitePool;
use crate::models::contact::Contact;
use chrono::Utc;

pub async fn list_contacts(pool: &SqlitePool) -> Result<Vec<Contact>> {
    let rows = sqlx::query!(
        "SELECT id, jid, name, push_name, phone_number, avatar_url, business_name, notes, tags, created_at, updated_at FROM contacts ORDER BY name"
    )
    .fetch_all(pool)
    .await?;

    Ok(rows
        .into_iter()
        .map(|r| Contact {
            id: r.id,
            jid: r.jid,
            name: r.name,
            push_name: r.push_name,
            phone_number: r.phone_number,
            avatar_url: r.avatar_url,
            business_name: r.business_name,
            notes: r.notes,
            tags: r.tags
                .map(|t| serde_json::from_str(&t).unwrap_or_default())
                .unwrap_or_default(),
            created_at: r.created_at.parse().unwrap_or_else(|_| Utc::now()),
            updated_at: r.updated_at.parse().unwrap_or_else(|_| Utc::now()),
        })
        .collect())
}

pub async fn upsert_contact(pool: &SqlitePool, c: &Contact) -> Result<()> {
    let tags = serde_json::to_string(&c.tags)?;
    let now = Utc::now().to_rfc3339();
    sqlx::query!(
        r#"
        INSERT INTO contacts (id, jid, name, push_name, phone_number, avatar_url,
                              business_name, notes, tags, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(jid) DO UPDATE SET
            name = excluded.name,
            push_name = excluded.push_name,
            phone_number = excluded.phone_number,
            avatar_url = excluded.avatar_url,
            business_name = excluded.business_name,
            notes = excluded.notes,
            tags = excluded.tags,
            updated_at = excluded.updated_at
        "#,
        c.id, c.jid, c.name, c.push_name, c.phone_number, c.avatar_url,
        c.business_name, c.notes, tags, now, now
    )
    .execute(pool)
    .await?;
    Ok(())
}
