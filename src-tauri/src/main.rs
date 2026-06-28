//! WhatsAuto — Tauri v2 application entry point
//!
//! This is the main binary crate for the Tauri desktop shell.
//! All application logic is implemented in the library crate (lib.rs).

#![cfg_attr(
    all(not(debug_assertions), target_os = "windows"),
    windows_subsystem = "windows"
)]

fn main() {
    whatsauto_lib::run();
}
