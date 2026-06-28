(ns whatsauto.events
  "re-frame event handlers.
   Events are the ONLY way to transition app state.
   Name convention: :domain/verb or :domain/verb-noun"
  (:require
   [re-frame.core :as rf]
   [whatsauto.db :as db]
   [whatsauto.effects :as fx]))

;; ============================================================
;; App lifecycle
;; ============================================================

(rf/reg-event-db
 :app/init
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 :app/ready
 (fn [{:keys [db]} _]
   {:db (assoc db :app/ready? true)
    ::fx/load-settings nil
    ::fx/load-chats nil
    ::fx/load-ai-providers nil}))

(rf/reg-event-db
 :app/set-error
 (fn [db [_ error]]
   (assoc db :app/error error)))

;; ============================================================
;; Navigation
;; ============================================================

(rf/reg-event-db
 :nav/set-view
 (fn [db [_ view]]
   (assoc db :nav/active-view view)))

(rf/reg-event-db
 :nav/set-panel
 (fn [db [_ panel]]
   (assoc db :nav/active-panel panel)))

(rf/reg-event-db
 :ui/toggle-sidebar
 (fn [db _]
   (update db :ui/sidebar-collapsed? not)))

;; ============================================================
;; WhatsApp connection
;; ============================================================

(rf/reg-event-db
 :wa/set-status
 (fn [db [_ status]]
   (assoc db :wa/status status)))

(rf/reg-event-db
 :wa/set-qr
 (fn [db [_ qr]]
   (-> db
       (assoc :wa/qr-code qr)
       (assoc :wa/status :qr))))

(rf/reg-event-db
 :wa/authenticated
 (fn [db _]
   (-> db
       (assoc :wa/status :connected)
       (assoc :wa/qr-code nil))))

(rf/reg-event-db
 :wa/disconnected
 (fn [db [_ reason]]
   (-> db
       (assoc :wa/status :disconnected)
       (assoc :wa/qr-code nil)
       (assoc :app/error (when reason (str "Disconnected: " reason))))))

;; ============================================================
;; Chats
;; ============================================================

(rf/reg-event-db
 :chats/loaded
 (fn [db [_ chats]]
   (-> db
       (assoc :chats/list chats)
       (assoc :chats/loading? false))))

(rf/reg-event-fx
 :chats/load
 (fn [{:keys [db]} _]
   {:db (assoc db :chats/loading? true)
    ::fx/invoke-command {:cmd "list_chats"
                         :on-success [:chats/loaded]
                         :on-error   [:app/set-error]}}))

(rf/reg-event-fx
 :chats/select
 (fn [{:keys [db]} [_ chat-id]]
   {:db (assoc db :chats/active-id chat-id)
    ::fx/invoke-command {:cmd "get_messages"
                         :args {:chat_id chat-id :limit 50}
                         :on-success [:messages/loaded chat-id]
                         :on-error   [:app/set-error]}}))

(rf/reg-event-db
 :chats/set-search
 (fn [db [_ q]]
   (assoc db :chats/search-query q)))

(rf/reg-event-db
 :chats/sync
 (fn [db [_ chats]]
   (assoc db :chats/list chats)))

;; ============================================================
;; Messages
;; ============================================================

(rf/reg-event-db
 :messages/loaded
 (fn [db [_ chat-id messages]]
   (-> db
       (assoc-in [:messages/by-chat chat-id] messages)
       (assoc :messages/loading? false))))

(rf/reg-event-db
 :messages/received
 (fn [db [_ msg]]
   (let [chat-id (:chatId msg)]
     (update-in db [:messages/by-chat chat-id]
                (fn [msgs]
                  (-> (vec (or msgs []))
                      (conj msg)))))))

(rf/reg-event-fx
 :messages/send-text
 (fn [{:keys [db]} [_ chat-id text]]
   {:db (assoc db :ui/compose-text "")
    ::fx/invoke-command {:cmd "send_text_message"
                         :args {:chat_id chat-id :text text}
                         :on-success [:messages/sent]
                         :on-error   [:app/set-error]}}))

(rf/reg-event-db
 :messages/sent
 (fn [db [_ _result]]
   db))

;; ============================================================
;; Contacts
;; ============================================================

(rf/reg-event-db
 :contacts/loaded
 (fn [db [_ contacts]]
   (-> db
       (assoc :contacts/list contacts)
       (assoc :contacts/loading? false))))

(rf/reg-event-fx
 :contacts/load
 (fn [{:keys [db]} _]
   {:db (assoc db :contacts/loading? true)
    ::fx/invoke-command {:cmd "list_contacts"
                         :on-success [:contacts/loaded]
                         :on-error   [:app/set-error]}}))

;; ============================================================
;; AI
;; ============================================================

(rf/reg-event-db
 :ai/providers-loaded
 (fn [db [_ providers]]
   (assoc db :ai/providers providers)))

(rf/reg-event-fx
 :ai/load-providers
 (fn [_ _]
   {::fx/invoke-command {:cmd "list_providers"
                         :on-success [:ai/providers-loaded]
                         :on-error   [:app/set-error]}}))

(rf/reg-event-db
 :ai/set-active-provider
 (fn [db [_ provider-id]]
   (assoc db :ai/active-provider provider-id)))

(rf/reg-event-db
 :ai/add-user-message
 (fn [db [_ content]]
   (update db :ai/conversation conj {:role "user" :content content})))

(rf/reg-event-db
 :ai/add-assistant-message
 (fn [db [_ content]]
   (update db :ai/conversation conj {:role "assistant" :content content})))

(rf/reg-event-fx
 :ai/send-message
 (fn [{:keys [db]} [_ text]]
   (let [provider-id (:ai/active-provider db)
         history     (:ai/conversation db)
         new-msg     {:role "user" :content text}
         messages    (conj history new-msg)]
     {:db (-> db
              (update :ai/conversation conj new-msg)
              (assoc :ai/loading? true))
      ::fx/invoke-command {:cmd "send_ai_message"
                           :args {:provider_id provider-id
                                  :messages messages}
                           :on-success [:ai/response-received]
                           :on-error   [:ai/error]}})))

(rf/reg-event-db
 :ai/response-received
 (fn [db [_ response]]
   (-> db
       (assoc :ai/loading? false)
       (update :ai/conversation conj {:role "assistant"
                                      :content (:content response)}))))

(rf/reg-event-db
 :ai/error
 (fn [db [_ err]]
   (-> db
       (assoc :ai/loading? false)
       (assoc :app/error (str "AI error: " err)))))

(rf/reg-event-db
 :ai/clear-conversation
 (fn [db _]
   (assoc db :ai/conversation [])))

;; ============================================================
;; Settings
;; ============================================================

(rf/reg-event-db
 :settings/loaded
 (fn [db [_ settings]]
   (assoc db :settings/data settings)))

(rf/reg-event-fx
 :settings/load
 (fn [_ _]
   {::fx/invoke-command {:cmd "get_settings"
                         :on-success [:settings/loaded]
                         :on-error   [:app/set-error]}}))

(rf/reg-event-fx
 :settings/save
 (fn [{:keys [db]} [_ settings]]
   {:db (assoc db :settings/data settings)
    ::fx/invoke-command {:cmd "save_settings"
                         :args {:settings settings}
                         :on-success [:settings/saved]
                         :on-error   [:app/set-error]}}))

(rf/reg-event-db
 :settings/saved
 (fn [db _] db))

;; ============================================================
;; UI state
;; ============================================================

(rf/reg-event-db
 :ui/set-compose-text
 (fn [db [_ text]]
   (assoc db :ui/compose-text text)))

(rf/reg-event-db
 :ui/typing-start
 (fn [db [_ chat-id]]
   (update db :ui/typing-chats conj chat-id)))

(rf/reg-event-db
 :ui/typing-stop
 (fn [db [_ chat-id]]
   (update db :ui/typing-chats disj chat-id)))

(rf/reg-event-db
 :ui/push-notification
 (fn [db [_ notification]]
   (update db :ui/notifications conj notification)))
