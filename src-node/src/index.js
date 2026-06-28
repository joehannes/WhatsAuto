/**
 * WhatsAuto Node.js sidecar — entry point.
 *
 * This process is spawned by the Tauri Rust backend.
 * It:
 *   1. Connects to the Rust bridge via WebSocket
 *   2. Initialises the WhatsApp Web.js client
 *   3. Forwards WA events → Rust
 *   4. Handles command requests from Rust → WA
 */

import { parseArgs } from './bridge/args.js';
import { createBridge } from './bridge/websocket.js';
import { createWhatsAppClient } from './whatsapp/client.js';

const args = parseArgs(process.argv.slice(2));
const port = args.port || 9999;

console.log(`[wa-sidecar] Starting, bridge port: ${port}`);

const bridge = createBridge(port);
const waClient = createWhatsAppClient(bridge);

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('[wa-sidecar] SIGTERM received, shutting down');
  await waClient.destroy().catch(() => {});
  bridge.close();
  process.exit(0);
});

process.on('SIGINT', async () => {
  await waClient.destroy().catch(() => {});
  bridge.close();
  process.exit(0);
});
