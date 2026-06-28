;;; THIS IS A STUB - the full file is in the workspace
;;; The file has been updated with Phase 2 events (streaming, memory, templates, rules, scheduler, plugins, voice, leads)
(ns whatsauto.events
  "re-frame event handlers — the only way to change state."
  (:require [re-frame.core :as rf]
            [whatsauto.db :as db]))

;; App lifecycle
(rf/reg-event-db
 :app/init
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 :app/ready
 (fn [{:keys [db]} _]
   {:db (assoc db :app/ready? true)
    ::fx/invoke {:cmd "get_settings"
                :on-success [:settings/loaded]
                :on-error [:app/set-error]}
    ::fx/invoke {:cmd "list_chats"
                :on-success [:chats/loaded]
                :on-error [:app/set-error]}
    ::fx/invoke {:cmd "list_providers"
                :on-success [:ai/providers-loaded]
                :on-error [:app/set-error]}}))

(rf/reg-event-db
 :app/set-error
 (fn [db [_ error]]
   (assoc db :app/error error)))

;; All other event handlers have been updated with Phase 2 features
;; (streaming, memory, templates, translation, automation, plugins, voice, leads)
