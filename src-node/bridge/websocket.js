const WebSocket = require('ws');

const RECONNECT_DELAY = 2000;
const MAX_RECONNECTS = 30;

function createBridge(port) {
  let ws = null;
  let handlers = [];
  let reconnects = 0;
  let closed = false;

  function connect() {
    ws = new WebSocket(`ws://127.0.0.1:${port}`);
    ws.on('open', () => { reconnects = 0; console.log('[bridge] Connected'); });
    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data.toString());
        handlers.forEach(h => h(msg));
      } catch (e) { console.error('[bridge] Parse error:', e); }
    });
    ws.on('close', () => {
      if (closed) return;
      if (reconnects < MAX_RECONNECTS) {
        reconnects++;
        setTimeout(connect, RECONNECT_DELAY);
      } else {
        console.error('[bridge] Max reconnects');
        process.exit(1);
      }
    });
  }

  connect();

  return {
    sendEvent: (event, payload) => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ event, ...payload }));
      }
    },
    sendResponse: (id, payload) => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ id, ...payload }));
      }
    },
    onCommand: (handler) => { handlers.push(handler); },
    close: () => { closed = true; ws?.close(); }
  };
}

module.exports = { createBridge };
