/**
 * WhatsApp Web.js client wrapper.
 *
 * Responsibilities:
 *   - Initialise whatsapp-web.js with session persistence
 *   - Forward WA events to the Rust bridge
 *   - Handle commands from the Rust bridge
 *
 * The session is stored in `.wa-session/` relative to the
 * process working directory (i.e. the app data dir at runtime).
 */

import { Client, LocalAuth, MessageMedia } from 'whatsapp-web.js';
import { handleCommand } from './commands.js';

/**
 * @param {ReturnType<import('../bridge/websocket.js').createBridge>} bridge
 */
export function createWhatsAppClient(bridge) {
  const client = new Client({
    authStrategy: new LocalAuth({
      dataPath: '.wa-session',
    }),
    puppeteer: {
      headless: true,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-accelerated-2d-canvas',
        '--no-first-run',
        '--no-zygote',
        '--single-process',
        '--disable-gpu',
      ],
    },
    // Human-like user agent to reduce detection
    userAgent:
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
  });

  // ============================================================
  // Lifecycle events
  // ============================================================

  client.on('qr', (qr) => {
    console.log('[wa] QR code received');
    bridge.sendEvent('qr_code', { qr });
  });

  client.on('authenticated', () => {
    console.log('[wa] Authenticated');
    bridge.sendEvent('authenticated');
  });

  client.on('auth_failure', (msg) => {
    console.error('[wa] Auth failure:', msg);
    bridge.sendEvent('disconnected', { reason: `auth_failure: ${msg}` });
  });

  client.on('ready', async () => {
    console.log('[wa] Client ready');
    bridge.sendEvent('authenticated');

    // Sync chats on first ready
    try {
      const chats = await client.getChats();
      const serialised = chats.slice(0, 100).map(serializeChat);
      bridge.sendEvent('chats_sync', { chats: serialised });
    } catch (e) {
      console.error('[wa] Failed to sync chats:', e);
    }
  });

  client.on('disconnected', (reason) => {
    console.warn('[wa] Disconnected:', reason);
    bridge.sendEvent('disconnected', { reason });
  });

  // ============================================================
  // Message events
  // ============================================================

  client.on('message', async (msg) => {
    bridge.sendEvent('message_received', { message: serializeMessage(msg) });
  });

  client.on('message_create', async (msg) => {
    if (msg.fromMe) {
      bridge.sendEvent('message_received', { message: serializeMessage(msg) });
    }
  });

  client.on('message_ack', (msg, ack) => {
    bridge.sendEvent('message_ack', { id: msg.id._serialised, ack });
  });

  client.on('message_reaction', (reaction) => {
    bridge.sendEvent('message_reaction', {
      id: reaction.id,
      emoji: reaction.reaction,
      senderId: reaction.senderId,
    });
  });

  // ============================================================
  // Presence / typing
  // ============================================================

  client.on('chat_update', (chat) => {
    if (chat.isTyping) {
      bridge.sendEvent('typing_start', { jid: chat.id._serialised });
    } else {
      bridge.sendEvent('typing_stop', { jid: chat.id._serialised });
    }
  });

  // ============================================================
  // Command handler — requests from Rust
  // ============================================================

  bridge.onCommand(async (msg) => {
    const { id, cmd, ...params } = msg;
    try {
      const result = await handleCommand(client, cmd, params);
      bridge.sendResponse(id, result);
    } catch (e) {
      console.error(`[wa] Command '${cmd}' failed:`, e);
      bridge.sendResponse(id, { error: e.message });
    }
  });

  // Start the WA client
  client.initialize().catch((e) => {
    console.error('[wa] Initialisation failed:', e);
    bridge.sendEvent('disconnected', { reason: String(e) });
  });

  return client;
}

// ============================================================
// Serializers
// ============================================================

function serializeChat(chat) {
  return {
    id: chat.id._serialised,
    name: chat.name,
    isGroup: chat.isGroup,
    unreadCount: chat.unreadCount,
    lastMessage: chat.lastMessage ? serializeMessage(chat.lastMessage) : null,
    archived: chat.archived,
    isMuted: chat.isMuted,
    timestamp: chat.timestamp,
  };
}

function serializeMessage(msg) {
  return {
    id: msg.id._serialised,
    chatId: msg.from,
    fromMe: msg.fromMe,
    author: msg.author || msg.from,
    body: msg.body,
    type: msg.type,
    timestamp: msg.timestamp,
    hasMedia: msg.hasMedia,
    isStarred: msg.isStarred,
    hasReaction: msg.hasReaction,
  };
}
