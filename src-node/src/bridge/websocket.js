/**
 * WebSocket bridge — connects back to the Rust backend.
 *
 * The Rust backend starts a WS server on localhost:PORT.
 * This module connects as a client and provides:
 *   - `sendEvent(event, payload)` — push unsolicited events to Rust
 *   - `onCommand(handler)` — register handler for commands from Rust
 *   - `sendResponse(id, payload)` — reply to a specific command request
 */

import WebSocket from 'ws';

const RECONNECT_DELAY_MS = 2000;
const MAX_RECONNECTS = 30;

/**
 * @param {number} port
 */
export function createBridge(port) {
  let ws = null;
  let commandHandlers = [];
  let reconnects = 0;
  let closed = false;

  function connect() {
    console.log(`[bridge] Connecting to ws://127.0.0.1:${port}`);
    ws = new WebSocket(`ws://127.0.0.1:${port}`);

    ws.on('open', () => {
      console.log('[bridge] Connected to Rust backend');
      reconnects = 0;
    });

    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data.toString());
        for (const handler of commandHandlers) {
          handler(msg);
        }
      } catch (e) {
        console.error('[bridge] Failed to parse message:', e);
      }
    });

    ws.on('close', () => {
      if (closed) return;
      console.warn('[bridge] Connection closed');
      if (reconnects < MAX_RECONNECTS) {
        reconnects++;
        setTimeout(connect, RECONNECT_DELAY_MS);
      } else {
        console.error('[bridge] Max reconnects reached, exiting');
        process.exit(1);
      }
    });

    ws.on('error', (err) => {
      // Silently retry — Rust server may not be ready yet
      if (reconnects === 0) {
        console.warn('[bridge] WS error (will retry):', err.message);
      }
    });
  }

  connect();

  return {
    /** Emit an unsolicited event to the Rust backend. */
    sendEvent(event, payload = {}) {
      if (!ws || ws.readyState !== WebSocket.OPEN) {
        console.warn('[bridge] Not connected, dropping event:', event);
        return;
      }
      ws.send(JSON.stringify({ event, ...payload }));
    },

    /** Reply to a specific command request from Rust. */
    sendResponse(id, payload = {}) {
      if (!ws || ws.readyState !== WebSocket.OPEN) return;
      ws.send(JSON.stringify({ id, ...payload }));
    },

    /** Register a command handler. Called for every message from Rust. */
    onCommand(handler) {
      commandHandlers.push(handler);
    },

    /** Close the connection permanently. */
    close() {
      closed = true;
      ws?.close();
    },
  };
}
