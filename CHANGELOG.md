# Changelog

All notable changes to WhatsAuto will be documented in this file.
This project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Initial project scaffold
- Tauri v2 application shell with Rust backend
- WhatsApp Web integration via Node.js sidecar (`whatsapp-web.js`)
- WebSocket bridge between Rust core and Node.js sidecar
- ClojureScript/re-frame frontend architecture
- ShadCN UI component library integration via React interop
- Custom dark-first theme with WhatsApp-inspired green accent
- Two primary views:
  - **Chats view**: Chat list, conversation pane, compose bar, typing indicators
  - **AI Assistant view**: Multi-provider AI chat + configuration panel
- Additional views: Contacts, Settings, QR login
- Navigation sidebar with all planned views (stubs for future)
- SQLite persistence via sqlx with full schema
- AI provider abstraction supporting 8 providers:
  - Qwen, OpenAI, Claude, DeepSeek, Ollama, LM Studio, OpenRouter, Gemini (stub)
- OS keychain integration for API key storage
- re-frame event/subscription architecture
- Tauri event forwarding from WhatsApp to frontend
- GitHub Actions CI (build + test) and release workflows
- Architecture, Developer, and AI Provider documentation

## [0.1.0] — TBD

Initial milestone: working WhatsApp Business clone foundation.
