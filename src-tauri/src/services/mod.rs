//! Services layer — business logic as pure async functions.
//!
//! Services orchestrate domain logic without depending on Tauri.
//! Each service module is independently testable.

pub mod ai;
pub mod whatsapp;
