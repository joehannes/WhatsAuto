(ns whatsauto.db
  "Application state database schema for WhatsAuto.
   All state is immutable. Only dispatch events to change."
  (:require [cljs.spec.alpha :as s]))

(def default-db
  {:app/ready? false
   :app/loading? false
   :app/error nil

   ;; Navigation
   :nav/active-view :chats
   :nav/active-panel nil

   ;; WhatsApp connection
   :wa/status :disconnected
   :wa/qr-code nil
   :wa/phone-number nil
   :wa/typing-chats #{}

   ;; Chats
   :chats/list []
   :chats/active-id nil
   :chats/loading? false
   :chats/search-query ""
   :chats/syncing? false

   ;; Messages
   :messages/by-chat {}
   :messages/loading? false
   :messages/selected-message nil

   ;; Contacts
   :contacts/list []
   :contacts/loading? false
   :contacts/search ""
   :contacts/syncing? false

   ;; AI
   :ai/providers []
   :ai/active-provider nil
   :ai/conversation []
   :ai/loading? false
   :ai/streaming? false
   :ai/streaming-content ""
   :ai/config-open? false
   :ai/prompt-templates []
   :ai/selected-template nil

   ;; Conversation memory
   :memory/data {} ;; keyed by chat-id
   :memory/loading? false
   :memory/visible? false

   ;; Automation
   :scheduler/tasks []
   :scheduler/loading? false
   :rules/list []
   :rules/loading? false
   :rules/editor-open? false
   :rules/editing nil

   ;; Translation
   :translation/enabled false
   :translation/profiles []
   :translation/active-profile nil
   :translation/translate-before-send? false

   ;; Plugins
   :plugins/list []
   :plugins/loading? false
   :plugins/sdk-open? false

   ;; Voice
   :voice/listening? false
   :voice/available? false
   :voice/stt-providers []
   :voice/tts-providers []

   ;; Lead discovery
   :leads/results []
   :leads/loading? false
   :leads/providers []

   ;; Settings
   :settings/data {:theme "system"
                   :language "en"
                   :notifications-enabled true
                   :sound-enabled true
                   :auto-reply-enabled false
                   :auto-reply-delay-seconds 5
                   :memory-enabled false
                   :memory-depth 10
                   :translation-enabled false
                   :default-translation-style "business"
                   :voice-enabled false
                   :plugins-enabled false
                   :scheduler-enabled false
                   :business-hours-start "09:00"
                   :business-hours-end "18:00"
                   :timezone "UTC"}

   ;; UI state
   :ui/compose-text ""
   :ui/sidebar-collapsed? false
   :ui/notifications []
   :ui/ai-panel "chat" ; "chat" | "templates" | "memory" | "config"
   :ui/modal-open? false
   :ui/modal-content nil
   :ui/drag-over? false

   ;; Automation UI
   :ui/automation-tab "scheduler" ; "scheduler" | "rules" | "workflows"
   })
