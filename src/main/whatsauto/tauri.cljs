(ns whatsauto.tauri
  "Thin wrapper over the Tauri v2 JS API.
   All Tauri interop is centralised here —
   never call js/window.__TAURI__ directly from app code."
  (:require ["@tauri-apps/api/core" :refer [invoke]]
            ["@tauri-apps/api/event" :refer [listen emit]]))

;; Re-export for use across the app
(def invoke invoke)
(def listen listen)
(def emit  emit)

(defn listen-all
  "Register Tauri event listeners for WhatsApp sidecar events.
   Call once at app startup to keep the re-frame store in sync."
  []
  ;; QR code
  (listen "wa:qr_code"
          (fn [e]
            (let [data (js->clj (.-payload e) :keywordize-keys true)]
              (js/re_frame.core.dispatch #js [":wa/set-qr" (:qr data)]))))

  ;; Auth
  (listen "wa:authenticated"
          (fn [_] (js/re_frame.core.dispatch #js [":wa/authenticated"])))

  ;; Disconnect
  (listen "wa:disconnected"
          (fn [e]
            (let [data (js->clj (.-payload e) :keywordize-keys true)]
              (js/re_frame.core.dispatch #js [":wa/disconnected" (:reason data)]))))

  ;; Incoming messages
  (listen "wa:message_received"
          (fn [e]
            (let [data (js->clj (.-payload e) :keywordize-keys true)]
              (js/re_frame.core.dispatch #js [":messages/received" (:message data)]))))

  ;; Typing
  (listen "wa:typing_start"
          (fn [e]
            (let [data (js->clj (.-payload e) :keywordize-keys true)]
              (js/re_frame.core.dispatch #js [":ui/typing-start" (:jid data)]))))

  (listen "wa:typing_stop"
          (fn [e]
            (let [data (js->clj (.-payload e) :keywordize-keys true)]
              (js/re_frame.core.dispatch #js [":ui/typing-stop" (:jid data)]))))

  ;; Chat sync
  (listen "wa:chats_sync"
          (fn [e]
            (let [data (js->clj (.-payload e) :keywordize-keys true)]
              (js/re_frame.core.dispatch #js [":chats/sync" (:chats data)])))))
