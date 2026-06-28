(ns whatsauto.db
  "Application database schema.
   The entire UI state lives in this single immutable map.
   Use `re-frame.core/subscribe` to read; never mutate directly."
  (:require [cljs.spec.alpha :as s]))

;; ============================================================
;; Initial state
;; ============================================================

(def default-db
  {:app/ready?          false
   :app/loading?        false
   :app/error           nil

   ;; Navigation
   :nav/active-view     :chats     ; :chats | :ai | :contacts | :settings | ...
   :nav/active-panel    nil        ; secondary panel within a view

   ;; WhatsApp connection
   :wa/status           :disconnected  ; :disconnected | :qr | :connected
   :wa/qr-code          nil
   :wa/phone-number     nil

   ;; Chats
   :chats/list          []
   :chats/active-id     nil
   :chats/loading?      false
   :chats/search-query  ""

   ;; Messages (keyed by chat-id)
   :messages/by-chat    {}
   :messages/loading?   false

   ;; Contacts
   :contacts/list       []
   :contacts/loading?   false
   :contacts/search     ""

   ;; AI
   :ai/providers        []
   :ai/active-provider  nil
   :ai/conversation     []         ; [{:role "user" :content "..."} ...]
   :ai/loading?         false
   :ai/config-open?     false

   ;; Settings
   :settings/data       {:theme "dark"
                         :language "en"
                         :notifications_enabled true
                         :sound_enabled true
                         :auto_reply_enabled false}

   ;; UI state
   :ui/compose-text     ""
   :ui/sidebar-collapsed? false
   :ui/typing-chats     #{}
   :ui/notifications    []})
