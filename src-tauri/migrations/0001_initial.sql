-- Migration 0001: initial schema
-- WhatsAuto SQLite database schema
-- Designed for extensibility: every table has a UUID primary key
-- and timestamps for audit purposes.

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

-- ============================================================
-- Core messaging tables
-- ============================================================

CREATE TABLE IF NOT EXISTS chats (
    id                  TEXT PRIMARY KEY NOT NULL,
    jid                 TEXT NOT NULL UNIQUE,
    name                TEXT NOT NULL DEFAULT '',
    avatar_url          TEXT,
    last_message_preview TEXT,
    last_message_at     TEXT,
    unread_count        INTEGER NOT NULL DEFAULT 0,
    is_archived         INTEGER NOT NULL DEFAULT 0,
    is_group            INTEGER NOT NULL DEFAULT 0,
    -- JSON array of label strings (application-side)
    labels              TEXT NOT NULL DEFAULT '[]',
    -- human | ai | hybrid
    conversation_mode   TEXT NOT NULL DEFAULT 'human',
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at          TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_chats_last_message ON chats(last_message_at DESC);

CREATE TABLE IF NOT EXISTS messages (
    id              TEXT PRIMARY KEY NOT NULL,
    chat_id         TEXT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    from_me         INTEGER NOT NULL DEFAULT 0,
    author          TEXT NOT NULL DEFAULT '',
    body            TEXT,
    -- text | image | video | audio | document | sticker | location | contact | reaction | system
    message_type    TEXT NOT NULL DEFAULT 'text',
    media_url       TEXT,
    media_mime      TEXT,
    timestamp       TEXT NOT NULL DEFAULT (datetime('now')),
    is_read         INTEGER NOT NULL DEFAULT 0,
    is_starred      INTEGER NOT NULL DEFAULT 0,
    reaction        TEXT,
    -- Which AI provider generated this message (NULL for human messages)
    ai_provider     TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_ts ON messages(chat_id, timestamp DESC);

-- ============================================================
-- Contacts
-- ============================================================

CREATE TABLE IF NOT EXISTS contacts (
    id              TEXT PRIMARY KEY NOT NULL,
    jid             TEXT NOT NULL UNIQUE,
    name            TEXT NOT NULL DEFAULT '',
    push_name       TEXT,
    phone_number    TEXT,
    avatar_url      TEXT,
    business_name   TEXT,
    notes           TEXT,
    -- JSON array of tag strings
    tags            TEXT NOT NULL DEFAULT '[]',
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ============================================================
-- AI providers
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_providers (
    id              TEXT PRIMARY KEY NOT NULL,
    -- qwen | openai | claude | gemini | deepseek | ollama | lmstudio | openrouter | custom
    kind            TEXT NOT NULL DEFAULT 'qwen',
    name            TEXT NOT NULL,
    base_url        TEXT NOT NULL DEFAULT '',
    model           TEXT NOT NULL DEFAULT '',
    -- Reference key for OS keychain entry (actual key never stored in DB)
    api_key_ref     TEXT,
    is_default      INTEGER NOT NULL DEFAULT 0,
    enabled         INTEGER NOT NULL DEFAULT 1,
    system_prompt   TEXT,
    max_tokens      INTEGER,
    temperature     REAL,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ============================================================
-- Prompt templates
-- ============================================================

CREATE TABLE IF NOT EXISTS prompt_templates (
    id              TEXT PRIMARY KEY NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    template        TEXT NOT NULL,
    -- JSON array of variable names
    variables       TEXT NOT NULL DEFAULT '[]',
    provider_id     TEXT REFERENCES ai_providers(id) ON DELETE SET NULL,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ============================================================
-- Application settings (singleton row)
-- ============================================================

CREATE TABLE IF NOT EXISTS settings (
    id                      INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
    -- light | dark | system
    theme                   TEXT NOT NULL DEFAULT 'system',
    language                TEXT NOT NULL DEFAULT 'en',
    notifications_enabled   INTEGER NOT NULL DEFAULT 1,
    sound_enabled           INTEGER NOT NULL DEFAULT 1,
    auto_reply_enabled      INTEGER NOT NULL DEFAULT 0,
    default_ai_provider_id  TEXT REFERENCES ai_providers(id) ON DELETE SET NULL,
    business_name           TEXT,
    business_phone          TEXT,
    updated_at              TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Insert default settings row
INSERT OR IGNORE INTO settings (id) VALUES (1);

-- ============================================================
-- Scheduled tasks (prepared for AI scheduler)
-- ============================================================

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id              TEXT PRIMARY KEY NOT NULL,
    chat_id         TEXT REFERENCES chats(id) ON DELETE CASCADE,
    -- send_message | ai_action | campaign_step
    task_type       TEXT NOT NULL,
    payload         TEXT NOT NULL DEFAULT '{}',
    -- pending | running | done | failed
    status          TEXT NOT NULL DEFAULT 'pending',
    scheduled_at    TEXT NOT NULL,
    executed_at     TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ============================================================
-- Audit log
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id          TEXT PRIMARY KEY NOT NULL,
    event_type  TEXT NOT NULL,
    payload     TEXT NOT NULL DEFAULT '{}',
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at DESC);
