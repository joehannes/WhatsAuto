const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');

function createWhatsAppClient(bridge) {
  const client = new Client({
    authStrategy: new LocalAuth({ dataPath: '.wa-session' }),
    puppeteer: {
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage',
             '--disable-accelerated-2d-canvas', '--no-first-run', '--no-zygote',
             '--single-process', '--disable-gpu']
    },
    userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36'
  });

  client.on('qr', (qr) => {
    console.log('[wa] QR received');
    bridge.sendEvent('qr_code', { qr });
  });

  client.on('authenticated', () => {
    console.log('[wa] Authenticated');
    bridge.sendEvent('authenticated');
  });

  client.on('ready', async () => {
    console.log('[wa] Ready');
    bridge.sendEvent('authenticated');
    try {
      const chats = await client.getChats();
      bridge.sendEvent('chats_sync', { chats: chats.slice(0, 100).map(c => ({
        id: c.id._serialized, name: c.name, isGroup: c.isGroup,
        unreadCount: c.unreadCount, timestamp: c.timestamp, archived: c.archived
      })) });
    } catch (e) { console.error('[wa] Chat sync failed:', e); }
  });

  client.on('message', (msg) => {
    bridge.sendEvent('message_received', {
      message: {
        id: msg.id._serialized, chatId: msg.from, fromMe: msg.fromMe,
        author: msg.author || msg.from, body: msg.body, type: msg.type,
        timestamp: msg.timestamp, hasMedia: msg.hasMedia, isStarred: msg.isStarred,
        hasReaction: msg.hasReaction, quotedMsgId: msg.quotedMsg?.id?._serialized
      }
    });
  });

  client.on('message_ack', (msg, ack) => {
    bridge.sendEvent('message_ack', { id: msg.id._serialized, ack });
  });

  client.on('message_reaction', (reaction) => {
    bridge.sendEvent('message_reaction', {
      id: reaction.id, emoji: reaction.reaction, senderId: reaction.senderId
    });
  });

  client.on('disconnected', (reason) => {
    console.warn('[wa] Disconnected:', reason);
    bridge.sendEvent('disconnected', { reason });
  });

  bridge.onCommand(async (msg) => {
    const { id, cmd, ...params } = msg;
    try {
      let result = {};
      switch (cmd) {
        case 'get_status': {
          const info = client.info;
          result = { status: info ? 'connected' : 'disconnected', phone: info?.wid?.user };
          break;
        }
        case 'send_text': {
          const m = await client.sendMessage(params.jid, params.text);
          result = { id: m.id._serialized, status: 'sent' };
          break;
        }
        case 'send_media': {
          const media = await MessageMedia.fromFilePath(params.path);
          const m = await client.sendMessage(params.jid, media, {
            caption: params.caption || undefined,
            sendAudioAsVoice: params.media_type === 'audio'
          });
          result = { id: m.id._serialized, status: 'sent' };
          break;
        }
        case 'send_reaction': {
          const chat = await client.getChatById(params.jid);
          const msg = await chat.fetchMessages({ limit: 1 });
          if (msg[0]) await msg[0].react(params.emoji);
          result = { status: 'reacted' };
          break;
        }
        case 'logout': {
          await client.logout();
          result = { status: 'logged_out' };
          break;
        }
        case 'get_chats': {
          const chats = await client.getChats();
          result = { chats: chats.slice(0, 200).map(c => ({
            id: c.id._serialized, name: c.name, isGroup: c.isGroup,
            unreadCount: c.unreadCount, timestamp: c.timestamp, archived: c.archived,
            pinned: c.pinned, labels: c.labels
          })) };
          break;
        }
        case 'get_messages': {
          const chat = await client.getChatById(params.jid);
          const msgs = await chat.fetchMessages({ limit: Number(params.limit || 50) });
          result = { messages: msgs.map(m => ({
            id: m.id._serialized, chatId: params.jid, fromMe: m.fromMe,
            author: m.author || m.from, body: m.body, type: m.type,
            timestamp: m.timestamp, hasMedia: m.hasMedia, isStarred: m.isStarred,
            reaction: m.reaction, quotedMsgId: m.quotedMsg?.id?._serialized
          })) };
          break;
        }
        case 'get_contacts': {
          const contacts = await client.getContacts();
          result = { contacts: contacts.map(c => ({
            id: c.id._serialized, jid: c.id._serialized, name: c.name || c.pushname || '',
            pushName: c.pushname, phoneNumber: c.number, isBusiness: c.isBusiness
          })) };
          break;
        }
        default:
          result = { error: `Unknown cmd: ${cmd}` };
      }
      bridge.sendResponse(id, result);
    } catch (e) {
      console.error(`[wa] ${cmd} failed:`, e.message);
      bridge.sendResponse(id, { error: e.message });
    }
  });

  client.initialize().catch(e => {
    console.error('[wa] Init failed:', e);
    bridge.sendEvent('disconnected', { reason: String(e) });
  });

  return client;
}

module.exports = { createWhatsAppClient };
