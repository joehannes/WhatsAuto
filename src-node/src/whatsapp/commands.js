/**
 * Command dispatcher — routes Rust requests to WA client calls.
 *
 * Each command returns a plain object that will be sent back
 * as the JSON response to the Rust request.
 */

import { MessageMedia } from 'whatsapp-web.js';

/**
 * @param {import('whatsapp-web.js').Client} client
 * @param {string} cmd
 * @param {Record<string, any>} params
 */
export async function handleCommand(client, cmd, params) {
  switch (cmd) {
    case 'get_qr': {
      // QR is delivered via events; just report status
      return { status: 'pending', qr: null };
    }

    case 'get_status': {
      const info = client.info;
      if (info) {
        return { status: 'connected', phone: info.wid.user };
      }
      return { status: 'disconnected' };
    }

    case 'send_text': {
      const { jid, text } = params;
      const msg = await client.sendMessage(jid, text);
      return { id: msg.id._serialised, status: 'sent' };
    }

    case 'send_media': {
      const { jid, path: mediaPath, caption, media_type } = params;
      const media = await MessageMedia.fromFilePath(mediaPath);
      const msg = await client.sendMessage(jid, media, {
        caption: caption || undefined,
        sendAudioAsVoice: media_type === 'audio',
      });
      return { id: msg.id._serialised, status: 'sent' };
    }

    case 'logout': {
      await client.logout();
      return { status: 'logged_out' };
    }

    case 'get_chats': {
      const chats = await client.getChats();
      return {
        chats: chats.slice(0, 200).map((c) => ({
          id: c.id._serialised,
          name: c.name,
          isGroup: c.isGroup,
          unreadCount: c.unreadCount,
          timestamp: c.timestamp,
          archived: c.archived,
        })),
      };
    }

    case 'get_messages': {
      const { jid, limit = 50 } = params;
      const chat = await client.getChatById(jid);
      const messages = await chat.fetchMessages({ limit: Number(limit) });
      return {
        messages: messages.map((m) => ({
          id: m.id._serialised,
          chatId: jid,
          fromMe: m.fromMe,
          author: m.author || m.from,
          body: m.body,
          type: m.type,
          timestamp: m.timestamp,
          hasMedia: m.hasMedia,
        })),
      };
    }

    default:
      throw new Error(`Unknown command: ${cmd}`);
  }
}
