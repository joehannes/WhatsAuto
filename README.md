# WhatsAuto

> AI-enhanced WhatsApp Business desktop client

WhatsAuto is a production-quality desktop application combining WhatsApp Business messaging with AI-powered automation capabilities. Built on **Tauri v2**, **ClojureScript/re-frame**, and **Rust**, it provides a modern, extensible platform for businesses that want intelligent messaging workflows.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    ClojureScript UI                     │
│              (re-frame + Reagent + ShadCN)              │
└──────────────────────┬──────────────────────────────────┘
                       │ Tauri IPC
┌──────────────────────▼──────────────────────────────────┐
│                   Rust Core (Tauri)                     │
│     Commands • Events • State • Storage • Config        │
└──────────────────────┬──────────────────────────────────┘
                       │ stdio / WebSocket
┌──────────────────────▼──────────────────────────────────┐
│              Node.js Sidecar Service                    │
│              (whatsapp-web.js + WS bridge)              │
└──────────────────────┬──────────────────────────────────┘
                       │ Puppeteer / CDP
┌──────────────────────▼──────────────────────────────────┐
│                  WhatsApp Web                           │
└─────────────────────────────────────────────────────────┘
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| Desktop shell | Tauri v2 |
| Backend | Rust + Tokio + sqlx |
| Frontend | ClojureScript + Shadow-CLJS |
| UI framework | re-frame + Reagent |
| Component library | ShadCN UI (via React interop) |
| Styling | Tailwind CSS v4 |
| WhatsApp | whatsapp-web.js (Node.js sidecar) |
| Database | SQLite via sqlx |

## Project Structure

```
WhatsAuto/
├── src-tauri/           # Rust Tauri backend
│   ├── src/
│   │   ├── main.rs      # Entry point
│   │   ├── commands/    # Tauri command handlers
│   │   ├── services/    # Business logic services
│   │   ├── storage/     # SQLite persistence
│   │   ├── bridge/      # Node sidecar IPC bridge
│   │   └── models/      # Domain types
│   └── Cargo.toml
├── src-node/            # Node.js whatsapp-web.js sidecar
│   ├── src/
│   │   ├── index.js     # Sidecar entry point
│   │   ├── whatsapp/    # WA client wrapper
│   │   └── bridge/      # IPC bridge to Rust
│   └── package.json
├── src/                 # ClojureScript frontend
│   ├── main/
│   │   └── whatsauto/
│   │       ├── core.cljs        # App entry + routing
│   │       ├── db.cljs          # re-frame app-db schema
│   │       ├── events.cljs      # re-frame events
│   │       ├── effects.cljs     # re-frame effects
│   │       ├── subs.cljs        # re-frame subscriptions
│   │       ├── views/           # UI components
│   │       │   ├── app.cljs     # Root layout
│   │       │   ├── sidebar.cljs # Navigation sidebar
│   │       │   ├── chats.cljs   # Chat list + view
│   │       │   ├── contacts.cljs
│   │       │   ├── ai.cljs      # AI interaction view
│   │       │   └── settings.cljs
│   │       └── interop/         # JS/React interop
│   │           └── shadcn.cljs  # ShadCN component wrappers
│   └── test/
├── shadow-cljs.edn      # Shadow-CLJS build config
├── package.json         # Frontend JS deps
├── tailwind.config.js   # Tailwind config
└── docs/                # Architecture + API docs
```

## Getting Started

### Prerequisites

- [Rust](https://rustup.rs/) (stable)
- [Node.js](https://nodejs.org/) 18+
- [Java](https://openjdk.org/) 11+ (for Clojure toolchain)
- [Clojure CLI](https://clojure.org/guides/install_clojure)
- [Tauri CLI v2](https://tauri.app/start/)

### Install dependencies

```bash
# Install JS deps (frontend + sidecar)
npm install

# Install sidecar deps
cd src-node && npm install && cd ..
```

### Development

```bash
# Start Shadow-CLJS watcher (compiles CLJS → JS)
npx shadow-cljs watch app

# In another terminal: start Tauri dev
npm run tauri:dev
```

### Production Build

```bash
npm run build
```

## Views

### 1. Messenger View (default)
The main WhatsApp Business interface — chat list, active conversation, contact info panel.

### 2. AI Assistant View
Configure AI providers, manage prompt templates, set per-conversation AI modes, and interact with the built-in AI assistant.

## Roadmap

- [x] Core architecture & scaffold
- [x] WhatsApp Web integration (sidecar)
- [x] Basic messaging UI
- [x] AI provider abstraction layer
- [ ] QR login flow
- [ ] Full message history
- [ ] AI auto-reply
- [ ] Campaign engine
- [ ] Lead discovery
- [ ] Translation layer
- [ ] Voice input
- [ ] Plugin SDK

## License

MIT — see [LICENSE](LICENSE)
