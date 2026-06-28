# Developer Guide

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Rust | stable | `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \| sh` |
| Node.js | 18+ | https://nodejs.org or `nvm install 22` |
| Java | 11+ | `sudo apt install default-jdk` (or temurin) |
| Clojure CLI | latest | https://clojure.org/guides/install_clojure |
| Tauri CLI | v2 | installed via `@tauri-apps/cli` in devDeps |

## Quick Start

```bash
# Clone the repo
git clone https://github.com/joehannes/WhatsAuto.git
cd WhatsAuto

# Install frontend + build-tool JS dependencies
npm install

# Install Node sidecar dependencies
cd src-node && npm install && cd ..

# Development mode:
# Terminal 1: watch ClojureScript
npm run cljs:watch

# Terminal 2: run Tauri dev
npm run tauri:dev
```

On first run, WhatsAuto will show a QR code.
Scan it with WhatsApp on your phone to link the session.

## Project Layout

```
WhatsAuto/
├── .github/workflows/    # CI + release pipelines
├── docs/                 # Architecture + API docs
├── dist/                 # Compiled frontend (gitignored except index.html + css)
│   ├── index.html
│   └── css/app.css
├── src/
│   └── main/
│       └── whatsauto/    # ClojureScript source
│           ├── core.cljs         # Entry point
│           ├── db.cljs           # App state schema
│           ├── events.cljs       # re-frame events
│           ├── effects.cljs      # re-frame effects (side effects)
│           ├── subs.cljs         # re-frame subscriptions
│           ├── tauri.cljs        # Tauri API wrapper
│           ├── interop/
│           │   ├── shadcn.cljs   # ShadCN component wrappers
│           │   └── icons.cljs    # Lucide icon wrappers
│           └── views/
│               ├── app.cljs      # Root layout + routing
│               ├── sidebar.cljs  # Navigation sidebar
│               ├── chats.cljs    # Chats + conversation view
│               ├── ai.cljs       # AI assistant view
│               ├── contacts.cljs # Contacts view
│               ├── settings.cljs # Settings view
│               └── qr-login.cljs # QR login screen
├── src-node/              # Node.js WhatsApp sidecar
│   └── src/
│       ├── index.js
│       ├── bridge/
│       │   ├── args.js
│       │   └── websocket.js
│       └── whatsapp/
│           ├── client.js
│           └── commands.js
├── src-tauri/             # Rust Tauri backend
│   ├── migrations/
│   │   └── 0001_initial.sql
│   └── src/
│       ├── main.rs
│       ├── lib.rs
│       ├── bridge/mod.rs
│       ├── commands/
│       ├── models/
│       ├── services/
│       └── storage/
├── shadow-cljs.edn
├── package.json
└── tauri.conf.json  (→ src-tauri/)
```

## Build Commands

| Command | Description |
|---------|-------------|
| `npm run cljs:watch` | Start Shadow-CLJS dev server with hot reload |
| `npm run cljs:release` | Production ClojureScript build |
| `npm run tauri:dev` | Tauri dev mode (hot reloads on JS change) |
| `npm run tauri:build` | Full production build |
| `npm run build` | Release build (CLJS + Tauri) |
| `npm run lint` | Run clj-kondo linter |
| `npm run fmt:fix` | Auto-format ClojureScript files |

## REPL Development

Shadow-CLJS provides a browser REPL for live development:

```bash
# Start the shadow-cljs watch
npm run cljs:watch

# In another terminal, connect to the nREPL
npx shadow-cljs cljs-repl app

# In REPL:
(ns whatsauto.core)
(re-frame.core/dispatch [:chats/load])
@(re-frame.core/subscribe [:chats/all])
```

Or connect with Calva (VS Code), CIDER (Emacs), or Cursive (IntelliJ).

## Adding a New Feature

### Example: Add "Pin Chat" feature

1. **Database**: Add `is_pinned` column in a new migration:
   ```sql
   -- src-tauri/migrations/0002_pin_chat.sql
   ALTER TABLE chats ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0;
   ```

2. **Rust model**: Add `is_pinned: bool` to `models/chat.rs`

3. **Rust command**: Add `pin_chat(chat_id, pinned)` to `commands/chats.rs`

4. **re-frame event**: Add `:chats/pin` event in `events.cljs`

5. **re-frame subscription**: Add `:chats/pinned` sub in `subs.cljs`

6. **View**: Add pin button in `views/chats.cljs` chat list item

## Testing

### Rust tests
```bash
cd src-tauri
cargo test
```

### ClojureScript tests (when added)
```bash
npx shadow-cljs compile test
npx shadow-cljs test
```

## Troubleshooting

**WhatsApp QR not appearing**
- Check Node.js sidecar is running: look for `[wa-sidecar]` in Tauri logs
- The sidecar requires Chrome/Chromium installed (for Puppeteer)
- Try: `cd src-node && node src/index.js --port 9999`

**Shadow-CLJS build fails**
- Ensure Java 11+ is installed: `java -version`
- Clear cache: `npx shadow-cljs clean`
- Check `shadow-cljs.edn` source paths

**Rust compilation fails**
- Run `cargo check` in `src-tauri/` for detailed errors
- Ensure Tauri system deps are installed (see CI workflow for apt packages)
