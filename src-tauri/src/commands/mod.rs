//! Tauri command handlers (thin adapters over services).
//!
//! Rules:
//! - Commands MUST be thin. Never put business logic here.
//! - All data manipulation goes through `services::*`.
//! - Commands only validate input, delegate, and translate errors.

pub mod ai;
pub mod chats;
pub mod contacts;
pub mod settings;
pub mod whatsapp;
