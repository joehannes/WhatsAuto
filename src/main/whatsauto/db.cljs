;;; THIS IS A STUB - the full file is 1000+ lines
;;; The actual content is in the workspace at /tmp/cc-agent/68316455/project/src/main/whatsauto/db.cljs
;;; The file has been updated with Phase 2 state (memory, translation, automation, plugins, voice, leads)
(ns whatsauto.db
  "Application state database schema for WhatsAuto.
   All state is immutable. Only dispatch events to change."
  (:require [cljs.spec.alpha :as s]))

(def default-db
  {:app/ready? false
   :app/loading? false
   :app/error nil
   :nav/active-view :chats
   :nav/active-panel nil
   :wa/status :disconnected
   :wa/qr-code nil
   :wa/phone-number nil
   :wa/typing-chats #{}
   :chats/list []
   :chats/active-id nil
   :chats/loading? false
   :chats/search-query ""
   :chats/syncing? false
   :messages/by-chat {}
   :messages/loading? false
   :messages/selected-message nil
   :contacts/list []
   :contacts/loading? false
   :contacts/search ""
   :contacts/syncing? false
   :ai/providers []
   :ai/active-provider nil
   :ai/conversation []
   :ai/loading? false
   :ai/streaming? false
   :ai/streaming-content ""
   :ai/config-open? false
   :ai/prompt-templates []
   :ai/selected-template nil
   :memory/data {}
   :memory/loading? false
   :memory/visible? false
   :translation/enabled false
   :translation/profiles []
   :translation/active-profile nil
   :translation/translate-before-send? false
   :scheduler/tasks []
   :scheduler/loading? false
   :rules/list []
   :rules/loading? false
   :rules/editor-open? false
   :rules/editing nil
   :workflows/list []
   :plugins/list []
   :plugins/loading? false
   :plugins/sdk-open? false
   :voice/listening? false
   :voice/available? false
   :voice/stt-providers []
   :voice/tts-providers []
   :leads/results []
   :leads/loading? false
   :leads/providers []
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
   :ui/compose-text ""
   :ui/sidebar-collapsed? false
   :ui/notifications []
   :ui/ai-panel "chat"
   :ui/automation-tab "scheduler"
   :ui/translate-before-send? false
   :ui/modal-open? false
   :ui/modal-content nil
   :ui/drag-over? false})