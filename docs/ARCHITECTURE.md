# Architecture Documentation

## Overview

WhatsAuto follows a strict layered architecture with clear boundaries between concerns.
Each layer communicates only with the layer directly above or below it.

```
┌─────────────────────────────────────────────────────────────┐
│                 ClojureScript UI Layer                       │
│                                                             │
│  Views (Reagent)  ──▶  re-frame Events                      │
│       ▲                      │                              │
│       │ re-frame Subs         ▼                              │
│  app-db (atom)  ◀──  re-frame Effects                       │
│                              │                              │
│                         Tauri IPC invoke()                  │
└──────────────────────────────┼──────────────────────────────┘
                               │  JSON (tauri::command)
┌──────────────────────────────▼──────────────────────────────┐
│                   Rust Core (Tauri v2)                       │
│                                                             │
│  Commands  ──▶  Services  ──▶  Storage (SQLite/sqlx)        │
│                     │                                        │
│                  Bridge  ◀──▶  BridgeSender                  │
│                     │         (WebSocket client)             │
└─────────────────────┼───────────────────────────────────────┘
                      │  WebSocket (localhost)
┌─────────────────────▼───────────────────────────────────────┐
│               Node.js Sidecar                                │
│                                                             │
│  WebSocket Bridge  ──▶  Command Handler  ──▶  WA Client     │
│                              ▲                   │           │
│  Event Emitter  ◀────────────┘           whatsapp-web.js    │
└──────────────────────────────────────────────────┼──────────┘
                                                   │ Puppeteer
                                            WhatsApp Web
```

## Layer Descriptions

### UI Layer (ClojureScript)

Implemented with **re-frame** (event-driven state management) and **Reagent** (React wrapper).

Key files:
- `src/main/whatsauto/db.cljs` — Application state schema (the single source of truth)
- `src/main/whatsauto/events.cljs` — Pure event handlers transforming db state
- `src/main/whatsauto/effects.cljs` — Side-effecting handlers (Tauri calls, DOM, timers)
- `src/main/whatsauto/subs.cljs` — Derived subscriptions (pure projections of db)
- `src/main/whatsauto/views/` — Reagent UI components
- `src/main/whatsauto/interop/` — JS/React interop adapters

Data flow:
```
User action → dispatch [:event/name args]
           → event-handler transforms db
           → effect-handler executes side effects
           → subscription signals re-render
           → view re-renders with new data
```

### Rust Core

The Tauri application shell. Responsibilities:
1. **Command handlers** (`src/commands/`) — thin glue between IPC and services
2. **Services** (`src/services/`) — business logic, AI provider adapters
3. **Storage** (`src/storage/`) — SQLite via sqlx, per-entity repositories
4. **Bridge** (`src/bridge/`) — WebSocket server + sidecar lifecycle management
5. **Models** (`src/models/`) — pure domain types (Serialize/Deserialize)

### Node.js Sidecar

A separate Node.js process managing the WhatsApp Web session via `whatsapp-web.js`.

Communication protocol: **JSON over WebSocket**

Request (Rust → Node):
```json
{"id": "uuid", "cmd": "send_text", "jid": "491701234567@c.us", "text": "Hello"}
```

Response (Node → Rust):
```json
{"id": "uuid", "status": "sent", "msgId": "WA-MESSAGE-ID"}
```

Event (Node → Rust, unsolicited):
```json
{"event": "message_received", "message": {...}}
```

Tauri then emits the event to the frontend via `app.emit("wa:message_received", payload)`.

## State Management

The entire frontend state lives in a single ClojureScript map (`app-db`).
It is never mutated directly — only through dispatched re-frame events.

Key state domains:

| Key prefix | Description |
|------------|-------------|
| `:app/*` | Global app state (ready, loading, errors) |
| `:nav/*` | Active view and panel |
| `:wa/*` | WhatsApp connection state |
| `:chats/*` | Chat list, active chat |
| `:messages/*` | Messages keyed by chat ID |
| `:contacts/*` | Contact list |
| `:ai/*` | AI providers, conversation, loading |
| `:settings/*` | Application settings |
| `:ui/*` | Ephemeral UI state (compose text, typing) |

## Data Flow: Incoming Message

```
1. WhatsApp Web delivers message to whatsapp-web.js
2. Node sidecar receives message event
3. Node emits WebSocket event: {event: "message_received", message: {...}}
4. Rust bridge receives WS message
5. Rust: app.emit("wa:message_received", &payload)
6. Tauri JS runtime fires event to ClojureScript
7. ClojureScript listener dispatches [:messages/received msg]
8. re-frame event handler updates app-db
9. Subscription :messages/active-chat signals change
10. Chat view re-renders with new message
```

## Database Schema

All persistence is SQLite via sqlx. See `src-tauri/migrations/0001_initial.sql` for the full schema.

Core tables:
- `chats` — WhatsApp conversations
- `messages` — Individual messages with type/media support
- `contacts` — Contact directory
- `ai_providers` — AI provider configuration (API keys stored in OS keychain)
- `prompt_templates` — Reusable AI prompt templates
- `settings` — Singleton application settings
- `scheduled_tasks` — Future AI scheduler tasks
- `audit_log` — Event audit trail

## Security Model

- **API keys** are stored in the OS keychain via the `keyring` crate, never in SQLite
- **WhatsApp session** is stored locally by `whatsapp-web.js` in `.wa-session/`
- **Tauri CSP** is configured for security; restrict to localhost in production
- The sidecar communicates only on localhost; no external WebSocket exposure

## AI Provider Abstraction

All providers implement the same interface in `src-tauri/src/services/ai.rs`:

```rust
pub async fn chat(provider: &AiProvider, messages: Vec<AiMessage>) -> Result<AiResponse>
```

Provider-specific adapters:
- `chat_openai_compat()` — OpenAI, Qwen, DeepSeek, Ollama, LM Studio, OpenRouter
- `chat_claude()` — Anthropic Claude (different request/response format)
- `chat_gemini()` — Google Gemini (stub, to be implemented)

To add a new provider: add a variant to `AiProviderKind`, add its `default_base_url()`,
and add a match arm in `chat()`.

## Extension Points

### Adding a new Tauri command
1. Add handler function in `src-tauri/src/commands/<module>.rs`
2. Register in `lib.rs` `generate_handler![]` macro
3. Call from ClojureScript via `(tauri/invoke "command_name" args)`
4. Dispatch result to re-frame event

### Adding a new view
1. Create `src/main/whatsauto/views/<name>.cljs`
2. Add nav item to `sidebar.cljs` nav-items vector
3. Add route in `app.cljs` active-view function

### Adding a new re-frame subscription
1. Add to `subs.cljs` with `rf/reg-sub`
2. Use in components with `@(rf/subscribe [:sub/name])`

### Adding a WhatsApp command
1. Add command case in `src-node/src/whatsapp/commands.js`
2. Add `BridgeRequest` variant in `src-tauri/src/bridge/mod.rs`
3. Add Tauri command in `src-tauri/src/commands/whatsapp.rs`
