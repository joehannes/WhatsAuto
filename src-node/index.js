#!/usr/bin/env node

// Node.js whatsapp-web.js sidecar
// Connects to Rust backend via WebSocket

const { parseArgs } = require('./bridge/args.js');
const { createBridge } = require('./bridge/websocket.js');
const { createWhatsAppClient } = require('./whatsapp/client.js');

const args = parseArgs(process.argv.slice(2));
const port = args.port || 9999;

console.log(`[wa-sidecar] Starting, bridge port: ${port}`);

const bridge = createBridge(port);
const waClient = createWhatsAppClient(bridge);

process.on('SIGTERM', async () => {
  await waClient.destroy().catch(() => {});
  bridge.close();
  process.exit(0);
});

process.on('SIGINT', async () => {
  await waClient.destroy().catch(() => {});
  bridge.close();
  process.exit(0);
});
