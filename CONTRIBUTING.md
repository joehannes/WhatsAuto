## Development

- Install: `make install` or `npm install && cd src-node && npm install`
- Dev: `npx shadow-cljs watch app` + `npx tauri dev`
- Build: `make build`
- Lint: `make lint`
- Tests: `make test`

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for full details.

```
ClojureScript/re-frame UI
       ↓ Tauri IPC
    Rust Core
       ↓ WebSocket
  Node.js Sidecar
       ↓ Puppeteer
  WhatsApp Web
```
