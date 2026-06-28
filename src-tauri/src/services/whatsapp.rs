//! WhatsApp service — high-level business logic over the bridge.
//!
//! This layer sits between commands and the raw bridge protocol.
//! Future: replace bridge calls with official Cloud API here
//! without touching the command layer.

use anyhow::Result;
use crate::bridge::{BridgeRequest, BridgeSender};
use crate::models::message::Message;
use crate::models::chat::Chat;

/// Send a text message and return the assigned message ID.
pub async fn send_text(
    bridge: &BridgeSender,
    jid: &str,
    text: &str,
) -> Result<String> {
    let resp = bridge
        .send_request(BridgeRequest::SendText {
            jid: jid.to_owned(),
            text: text.to_owned(),
        })
        .await?;

    Ok(resp
        .get("id")
        .and_then(|v| v.as_str())
        .unwrap_or_default()
        .to_owned())
}

/// Poll the current QR / connection status from the sidecar.
pub async fn get_status(bridge: &BridgeSender) -> Result<String> {
    let resp = bridge.send_request(BridgeRequest::GetStatus).await?;
    Ok(resp
        .get("status")
        .and_then(|v| v.as_str())
        .unwrap_or("disconnected")
        .to_owned())
}
