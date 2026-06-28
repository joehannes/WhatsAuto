//! WhatsAuto library crate — assembles all subsystems and registers Tauri commands.
//!
//! Architecture layers (bottom to top):
//! 1. `storage`   — SQLite persistence via sqlx
//! 2. `models`    — domain types (pure data)
//! 3. `services`  — business logic (pure functions + async)
//! 4. `bridge`    — Node.js sidecar IPC bridge (WebSocket)
//! 5. `commands`  — Tauri command handlers (thin glue)

pub mod bridge;
pub mod commands;
pub mod models;
pub mod services;
pub mod storage;

use tauri::Manager;
use tracing::info;

/// Application-wide shared state injected into Tauri's managed state.
pub struct AppState {
    pub db: storage::Database,
    pub bridge_tx: bridge::BridgeSender,
}

/// Entry point called from `main.rs`.
///
/// Sets up logging, initialises the database, starts the sidecar bridge
/// and registers all Tauri commands before running the event loop.
pub fn run() {
    // Initialise structured logging
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "whatsauto=debug,tauri=info".parse().unwrap()),
        )
        .init();

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_store::Builder::default().build())
        .plugin(tauri_plugin_http::init())
        .setup(|app| {
            let app_handle = app.handle().clone();

            tauri::async_runtime::block_on(async move {
                // Resolve the app data directory for database storage
                let data_dir = app_handle
                    .path()
                    .app_data_dir()
                    .expect("failed to resolve app data dir");
                std::fs::create_dir_all(&data_dir)
                    .expect("failed to create app data dir");

                let db_path = data_dir.join("whatsauto.db");
                info!("Opening database at {}", db_path.display());

                let db = storage::Database::connect(&db_path)
                    .await
                    .expect("failed to open database");

                db.migrate().await.expect("database migration failed");

                // Start the WhatsApp sidecar bridge
                let bridge_tx = bridge::start(app_handle.clone())
                    .await
                    .expect("failed to start sidecar bridge");

                app_handle.manage(AppState { db, bridge_tx });

                info!("WhatsAuto initialised successfully");
                Ok::<_, Box<dyn std::error::Error>>(())
            })
            .expect("async setup failed");

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            // WhatsApp commands
            commands::whatsapp::get_qr_code,
            commands::whatsapp::get_connection_status,
            commands::whatsapp::send_text_message,
            commands::whatsapp::send_media_message,
            commands::whatsapp::logout,
            // Chat commands
            commands::chats::list_chats,
            commands::chats::get_chat,
            commands::chats::get_messages,
            commands::chats::archive_chat,
            commands::chats::set_chat_label,
            commands::chats::star_message,
            // Contact commands
            commands::contacts::list_contacts,
            commands::contacts::get_contact,
            commands::contacts::upsert_contact,
            // AI commands
            commands::ai::list_providers,
            commands::ai::save_provider,
            commands::ai::delete_provider,
            commands::ai::send_ai_message,
            commands::ai::get_conversation_mode,
            commands::ai::set_conversation_mode,
            // Settings commands
            commands::settings::get_settings,
            commands::settings::save_settings,
        ])
        .run(tauri::generate_context!())
        .expect("error while running WhatsAuto");
}
