PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS chats (
    id TEXT PRIMARY KEY NOT NULL,
    jid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    last_message_preview TEXT,
    last_message_at TEXT,
    unread_count INTEGER NOT NULL DEFAULT 0,
    is_archived INTEGER NOT NULL DEFAULT 0,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    is_group INTEGER NOT NULL DEFAULT 0,
    labels TEXT NOT NULL DEFAULT '[]',
    conversation_mode TEXT NOT NULL DEFAULT 'human',
    ai_provider_id TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_chats_last_message ON chats(last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_chats_jid ON chats(jid);

CREATE TABLE IF NOT EXISTS messages (
    id TEXT PRIMARY KEY NOT NULL,
    chat_id TEXT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    from_me INTEGER NOT NULL DEFAULT 0,
    author TEXT NOT NULL DEFAULT '',
    body TEXT,
    message_type TEXT NOT NULL DEFAULT 'text',
    media_url TEXT,
    media_mime TEXT,
    media_file_name TEXT,
    media_size INTEGER,
    timestamp TEXT NOT NULL DEFAULT (datetime('now')),
    is_read INTEGER NOT NULL DEFAULT 0,
    is_starred INTEGER NOT NULL DEFAULT 0,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    reaction TEXT,
    quoted_message_id TEXT,
    ai_provider_id TEXT,
    ai_confidence REAL,
    schedule_id TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_ts ON messages(chat_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_messages_ai ON messages(ai_provider_id);

CREATE TABLE IF NOT EXISTS contacts (
    id TEXT PRIMARY KEY NOT NULL,
    jid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL DEFAULT '',
    push_name TEXT,
    phone_number TEXT,
    avatar_url TEXT,
    business_name TEXT,
    business_category TEXT,
    notes TEXT,
    tags TEXT NOT NULL DEFAULT '[]',
    language TEXT,
    is_business INTEGER NOT NULL DEFAULT 0,
    is_verified INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_contacts_name ON contacts(name);
CREATE INDEX IF NOT EXISTS idx_contacts_jid ON contacts(jid);

CREATE TABLE IF NOT EXISTS ai_providers (
    id TEXT PRIMARY KEY NOT NULL,
    kind TEXT NOT NULL DEFAULT 'qwen',
    name TEXT NOT NULL,
    base_url TEXT NOT NULL DEFAULT '',
    model TEXT NOT NULL DEFAULT '',
    api_key_ref TEXT,
    is_default INTEGER NOT NULL DEFAULT 0,
    enabled INTEGER NOT NULL DEFAULT 1,
    system_prompt TEXT,
    max_tokens INTEGER,
    temperature REAL,
    top_p REAL,
    streaming_enabled INTEGER NOT NULL DEFAULT 0,
    request_timeout INTEGER,
    retry_count INTEGER DEFAULT 3,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_ai_providers_enabled ON ai_providers(enabled);

CREATE TABLE IF NOT EXISTS prompt_templates (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL DEFAULT 'general',
    tags TEXT NOT NULL DEFAULT '[]',
    template TEXT NOT NULL,
    variables TEXT NOT NULL DEFAULT '[]',
    provider_id TEXT REFERENCES ai_providers(id) ON DELETE SET NULL,
    is_builtin INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_prompts_category ON prompt_templates(category);

CREATE TABLE IF NOT EXISTS conversation_memory (
    id TEXT PRIMARY KEY NOT NULL,
    chat_id TEXT NOT NULL UNIQUE,
    summary TEXT,
    facts TEXT NOT NULL DEFAULT '[]',
    user_preferences TEXT NOT NULL DEFAULT '[]',
    business_context TEXT,
    ai_notes TEXT NOT NULL DEFAULT '[]',
    memory_depth INTEGER NOT NULL DEFAULT 10,
    last_updated TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_memory_chat ON conversation_memory(chat_id);

CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
    theme TEXT NOT NULL DEFAULT 'system',
    language TEXT NOT NULL DEFAULT 'en',
    notifications_enabled INTEGER NOT NULL DEFAULT 1,
    sound_enabled INTEGER NOT NULL DEFAULT 1,
    auto_reply_enabled INTEGER NOT NULL DEFAULT 0,
    auto_reply_provider_id TEXT,
    auto_reply_delay_seconds INTEGER DEFAULT 5,
    default_ai_provider_id TEXT,
    business_name TEXT,
    business_phone TEXT,
    business_email TEXT,
    memory_enabled INTEGER NOT NULL DEFAULT 0,
    memory_depth INTEGER DEFAULT 10,
    translation_enabled INTEGER NOT NULL DEFAULT 0,
    default_translation_style TEXT DEFAULT 'business',
    voice_enabled INTEGER NOT NULL DEFAULT 0,
    voice_stt_provider TEXT,
    voice_tts_provider TEXT,
    plugins_enabled INTEGER NOT NULL DEFAULT 0,
    scheduler_enabled INTEGER NOT NULL DEFAULT 0,
    business_hours_start TEXT DEFAULT '09:00',
    business_hours_end TEXT DEFAULT '18:00',
    timezone TEXT DEFAULT 'UTC',
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

INSERT OR IGNORE INTO settings (id) VALUES (1);

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id TEXT PRIMARY KEY NOT NULL,
    chat_id TEXT REFERENCES chats(id) ON DELETE CASCADE,
    task_type TEXT NOT NULL,
    payload TEXT NOT NULL DEFAULT '{}',
    status TEXT NOT NULL DEFAULT 'pending',
    scheduled_at TEXT NOT NULL,
    executed_at TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_status ON scheduled_tasks(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_at ON scheduled_tasks(scheduled_at);

CREATE TABLE IF NOT EXISTS rules (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    is_enabled INTEGER NOT NULL DEFAULT 1,
    conditions TEXT NOT NULL DEFAULT '{}',
    actions TEXT NOT NULL DEFAULT '[]',
    priority INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_rules_enabled ON rules(is_enabled);

CREATE TABLE IF NOT EXISTS audit_log (
    id TEXT PRIMARY KEY NOT NULL,
    event_type TEXT NOT NULL,
    source TEXT,
    payload TEXT NOT NULL DEFAULT '{}',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_audit_type ON audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at DESC);

CREATE TABLE IF NOT EXISTS plugins (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    kind TEXT NOT NULL,
    config TEXT NOT NULL DEFAULT '{}',
    is_enabled INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS plugin_hooks (
    id TEXT PRIMARY KEY NOT NULL,
    plugin_id TEXT NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
    hook_name TEXT NOT NULL,
    config TEXT NOT NULL DEFAULT '{}',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS lead_discovery_results (
    id TEXT PRIMARY KEY NOT NULL,
    provider TEXT NOT NULL,
    query TEXT NOT NULL,
    results TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_leads_provider ON lead_discovery_results(provider);

CREATE TABLE IF NOT EXISTS translation_profiles (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    style TEXT NOT NULL DEFAULT 'business',
    tone TEXT NOT NULL DEFAULT 'professional',
    source_lang TEXT NOT NULL DEFAULT 'auto',
    target_lang TEXT NOT NULL,
    system_prompt TEXT,
    is_default INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS automation_workflows (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    trigger TEXT NOT NULL,
    steps TEXT NOT NULL DEFAULT '[]',
    is_enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS voice_config (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    model TEXT,
    voice_id TEXT,
    is_stt INTEGER NOT NULL DEFAULT 0,
    is_tts INTEGER NOT NULL DEFAULT 0,
    config TEXT NOT NULL DEFAULT '{}',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
