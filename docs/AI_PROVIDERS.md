# AI Provider Integration Guide

## Overview

WhatsAuto supports multiple AI providers through a unified abstraction layer.
All providers are configured in the UI (Settings → AI Providers) and
API keys are stored securely in the OS keychain.

## Supported Providers

| Provider | Kind | API Compat | Status |
|----------|------|------------|--------|
| Qwen (Alibaba) | `qwen` | OpenAI | ✅ Ready |
| OpenAI | `openai` | OpenAI | ✅ Ready |
| DeepSeek | `deepseek` | OpenAI | ✅ Ready |
| Ollama | `ollama` | OpenAI | ✅ Ready (local) |
| LM Studio | `lmstudio` | OpenAI | ✅ Ready (local) |
| OpenRouter | `openrouter` | OpenAI | ✅ Ready |
| Anthropic Claude | `claude` | Claude API | ✅ Ready |
| Google Gemini | `gemini` | Gemini | 🚧 Stub |

## Adding a Provider

### UI

1. Navigate to **AI** view in WhatsAuto
2. Click **+** in the providers panel
3. Select provider type, enter name, model, and API key
4. Click **Save Provider**
5. Click the provider card to make it active

### Configuration Reference

| Field | Description | Example |
|-------|-------------|--------|
| Kind | Provider type | `qwen` |
| Name | Display name | "My Qwen" |
| Base URL | API endpoint | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Model | Model identifier | `qwen-plus` |
| API Key | Provider API key (stored in keychain) | `sk-...` |
| System Prompt | Default system message | "You are a helpful assistant" |
| Max Tokens | Max response length | `4096` |
| Temperature | Creativity (0-2) | `0.7` |

## Qwen Quick Start

1. Register at https://dashscope.aliyuncs.com
2. Create an API key
3. In WhatsAuto: Add provider → Qwen → model: `qwen-plus`
4. Enter your API key

## Ollama Quick Start (local)

1. Install Ollama: https://ollama.ai
2. Pull a model: `ollama pull llama3.2`
3. Start Ollama: `ollama serve`
4. In WhatsAuto: Add provider → Ollama → model: `llama3.2`
5. No API key needed

## Conversation Modes

Each WhatsApp chat can be in one of three modes:

| Mode | Description |
|------|-------------|
| **Human** | You reply manually (default) |
| **AI** | AI replies automatically to all messages |
| **Hybrid** | AI suggests replies; you approve before sending |

Switch mode via the AI icon button in the conversation header.
The current mode is always visible as a badge in the chat.

## Future AI Capabilities

These are planned and have placeholder interfaces:

- **Prompt Templates** — Reusable prompts with variable substitution
- **Conversation Memory** — Persistent context across sessions
- **Auto-Translation** — Per-contact language profiles
- **Campaign Engine** — Batch AI-assisted outreach
- **Lead Discovery** — AI-powered contact finding
- **Voice Input** — Speech-to-text for hands-free operation
- **Background Agents** — Long-running AI tasks
- **Tool Calling** — AI controls app functions via natural language
