//! Domain model types.
//!
//! These are plain data types (POD) — no business logic here.
//! All types implement Serialize + Deserialize for Tauri IPC transport.

pub mod ai;
pub mod chat;
pub mod contact;
pub mod message;
pub mod settings;
