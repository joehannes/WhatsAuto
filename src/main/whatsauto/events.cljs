(ns whatsauto.events
  "re-frame event handlers — the only way to change state."
  (:require
   [re-frame.core :as rf]
   [whatsauto.db :as db]))

(defn- invoke-cmd
  [cmd & {:keys [args on-success on-error]}]
  [:whatsauto.effects/invoke-command
   {:cmd cmd
    :args args
    :on-success (or on-success [:app/noop])
    :on-error (or on-error [:app/set-error])}])

;; ============================================================
;; App lifecycle
;; ============================================================

(rf/reg-event-db
 :app/init
 (fn [_ _]
   db/default-db))

(rf/reg-event-db
 :app/noop
 (fn [db _]
   db))

(rf/reg-event-fx
 :app/ready
 (fn [{:keys [db]} _]
   {:db (assoc db :app/ready? true)
    :fx [[:dispatch [:wa/refresh-status]]
         (invoke-cmd "get_settings" :on-success [:settings/loaded])
         (invoke-cmd "list_chats" :on-success [:chats/loaded])
         (invoke-cmd "list_providers" :on-success [:ai/providers-loaded])
         (invoke-cmd "list_contacts" :on-success [:contacts/loaded])
         (invoke-cmd "list_prompt_templates" :on-success [:ai/templates-loaded])
         (invoke-cmd "list_scheduled_tasks" :on-success [:scheduler/loaded])
         (invoke-cmd "list_rules" :on-success [:rules/loaded])
         (invoke-cmd "list_plugins" :on-success [:plugins/loaded])]}))

(rf/reg-event-db
 :app/set-error
 (fn [db [_ error]]
   (assoc db :app/error error)))

;; ============================================================
;; Navigation & UI
;; ============================================================

(rf/reg-event-db
 :nav/set-view
 (fn [db [_ view]]
   (assoc db :nav/active-view view)))

(rf/reg-event-db
 :ui/set-compose-text
 (fn [db [_ text]]
   (assoc db :ui/compose-text text)))

(rf/reg-event-db
 :ui/typing-start
 (fn [db [_ jid]]
   (update db :wa/typing-chats conj jid)))

(rf/reg-event-db
 :ui/typing-stop
 (fn [db [_ jid]]
   (update db :wa/typing-chats disj jid)))

(rf/reg-event-db
 :automation/set-tab
 (fn [db [_ tab]]
   (assoc db :ui/automation-tab (name tab))))

;; ============================================================
;; WhatsApp connection
;; ============================================================

(rf/reg-event-fx
 :wa/refresh-status
 (fn [_ _]
   {:fx [(invoke-cmd "get_connection_status" :on-success [:wa/status-loaded])]}))

(rf/reg-event-db
 :wa/status-loaded
 (fn [db [_ status]]
   (assoc db :wa/status (keyword status))))

(rf/reg-event-db
 :wa/set-qr
 (fn [db [_ qr]]
   (-> db
       (assoc :wa/qr-code qr :wa/status :qr))))

(rf/reg-event-db
 :wa/authenticated
 (fn [db _]
   (assoc db :wa/status :connected :wa/qr-code nil)))

(rf/reg-event-db
 :wa/disconnected
 (fn [db [_ _reason]]
   (assoc db :wa/status :disconnected :wa/qr-code nil)))

(rf/reg-event-fx
 :wa/connect
 (fn [_ _]
   {:fx [(invoke-cmd "get_qr_code" :on-success [:wa/qr-loaded])]}))

(rf/reg-event-db
 :wa/qr-loaded
 (fn [db [_ resp]]
   (assoc db :wa/qr-code (:qr resp) :wa/status :qr)))

(rf/reg-event-fx
 :wa/refresh-qr
 (fn [_ _]
   {:fx [(invoke-cmd "get_qr_code" :on-success [:wa/qr-loaded])]}))

;; ============================================================
;; Chats & messages
;; ============================================================

(rf/reg-event-fx
 :chats/load
 (fn [_ _]
   {:fx [(invoke-cmd "list_chats" :on-success [:chats/loaded])]}))

(rf/reg-event-db
 :chats/loaded
 (fn [db [_ chats]]
   (assoc db :chats/list chats :chats/loading? false)))

(rf/reg-event-db
 :chats/sync
 (fn [db [_ chats]]
   (assoc db :chats/list chats :chats/syncing? false)))

(rf/reg-event-db
 :chats/select
 (fn [db [_ chat-id]]
   (assoc db :chats/active-id chat-id :messages/loading? true)))

(rf/reg-event-db
 :chats/set-search
 (fn [db [_ q]]
   (assoc db :chats/search-query q)))

(rf/reg-event-fx
 :messages/send-text
 (fn [{:keys [db]} [_ chat-id text]]
   (when (seq text)
     {:db (assoc db :ui/compose-text "")
      :fx [(invoke-cmd "send_text_message"
                       :args {:chatId chat-id :text text}
                       :on-success [:messages/sent chat-id])]})))

(rf/reg-event-db
 :messages/sent
 (fn [db [_ chat-id msg]]
   (update db :messages/by-chat update chat-id (fnil conj []) msg)))

(rf/reg-event-db
 :messages/received
 (fn [db [_ msg]]
   (let [chat-id (:chatId msg)]
     (update db :messages/by-chat update chat-id (fnil conj []) msg))))

;; ============================================================
;; Contacts
;; ============================================================

(rf/reg-event-db
 :contacts/loaded
 (fn [db [_ contacts]]
   (assoc db :contacts/list contacts :contacts/loading? false)))

(rf/reg-event-db
 :contacts/set-search
 (fn [db [_ q]]
   (assoc db :contacts/search q)))

;; ============================================================
;; Settings
;; ============================================================

(rf/reg-event-db
 :settings/loaded
 (fn [db [_ settings]]
   (assoc db :settings/data settings)))

(rf/reg-event-fx
 :settings/save
 (fn [{:keys [db]} [_ settings]]
   {:fx [(invoke-cmd "save_settings"
                     :args settings
                     :on-success [:settings/loaded])]}))

;; ============================================================
;; AI
;; ============================================================

(rf/reg-event-db
 :ai/providers-loaded
 (fn [db [_ providers]]
   (assoc db :ai/providers providers)))

(rf/reg-event-db
 :ai/templates-loaded
 (fn [db [_ templates]]
   (assoc db :ai/prompt-templates templates)))

(rf/reg-event-db
 :ai/set-active-provider
 (fn [db [_ provider-id]]
   (assoc db :ai/active-provider provider-id)))

(rf/reg-event-fx
 :ai/send-message
 (fn [{:keys [db]} [_ text]]
   (let [provider-id (:ai/active-provider db)]
     (when (and (seq text) provider-id)
       {:db (-> db
                (update :ai/conversation conj {:role "user" :content text})
                (assoc :ai/loading? true))
        :fx [(invoke-cmd "send_ai_message"
                         :args {:providerId provider-id :message text}
                         :on-success [:ai/response-received])]}))))

(rf/reg-event-db
 :ai/response-received
 (fn [db [_ resp]]
   (-> db
       (update :ai/conversation conj {:role "assistant" :content (:content resp)})
       (assoc :ai/loading? false :ai/streaming? false :ai/streaming-content ""))))

(rf/reg-event-db
 :ai/clear-conversation
 (fn [db _]
   (assoc db :ai/conversation [] :ai/streaming-content "")))

(rf/reg-event-fx
 :ai/save-provider
 (fn [_ [_ provider]]
   {:fx [(invoke-cmd "save_provider"
                     :args provider
                     :on-success [:ai/providers-loaded])]}))

(rf/reg-event-fx
 :ai/set-conversation-mode
 (fn [_ [_ chat-id mode]]
   {:fx [(invoke-cmd "set_chat_conversation_mode"
                     :args {:chatId chat-id :mode mode}
                     :on-success [:chats/load])]}))

;; ============================================================
;; Translation, plugins, scheduler, rules
;; ============================================================

(rf/reg-event-db
 :translation/toggle-enabled
 (fn [db _]
   (update-in db [:settings/data :translation-enabled] not)))

(rf/reg-event-db
 :translation/toggle-before-send
 (fn [db _]
   (update db :translation/translate-before-send? not)))

(rf/reg-event-db
 :plugins/loaded
 (fn [db [_ plugins]]
   (assoc db :plugins/list plugins)))

(rf/reg-event-db
 :plugins/toggle-enabled
 (fn [db [_ plugin-id]]
   (update db :plugins/list
           (fn [plugins]
             (mapv (fn [p]
                     (if (= (:id p) plugin-id)
                       (update p :enabled not)
                       p))
                   plugins)))))

(rf/reg-event-db
 :scheduler/loaded
 (fn [db [_ tasks]]
   (assoc db :scheduler/tasks tasks)))

(rf/reg-event-db
 :rules/loaded
 (fn [db [_ rules]]
   (assoc db :rules/list rules)))
