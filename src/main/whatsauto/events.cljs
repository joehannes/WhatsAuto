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

;; Navigation
(rf/reg-event-db
 :nav/set-view
 (fn [db [_ view]]
   (assoc db :nav/active-view view)))

(rf/reg-event-db
 :ui/toggle-sidebar
 (fn [db _]
   (update db :ui/sidebar-collapsed? not)))

;; WhatsApp
(rf/reg-event-db
 :wa/set-status
 (fn [db [_ status]]
   (assoc db :wa/status status)))

(rf/reg-event-db
 :wa/set-qr
 (fn [db [_ qr]]
   (-> db (assoc :wa/qr-code qr) (assoc :wa/status :qr))))

(rf/reg-event-db
 :wa/authenticated
 (fn [db _]
   (-> db (assoc :wa/status :connected) (assoc :wa/qr-code nil))))

(rf/reg-event-db
 :wa/disconnected
 (fn [db [_ reason]]
   (-> db (assoc :wa/status :disconnected) (assoc :wa/qr-code nil)
       (assoc :app/error (when reason (str "Disconnected: " reason))))))

(rf/reg-event-db
 :wa/typing-start
 (fn [db [_ chat-id]]
   (update db :wa/typing-chats conj chat-id)))

(rf/reg-event-db
 :wa/typing-stop
 (fn [db [_ chat-id]]
   (update db :wa/typing-chats disj chat-id)))

;; Chats
(rf/reg-event-db
 :chats/loaded
 (fn [db [_ chats]]
   (-> db (assoc :chats/list chats) (assoc :chats/loading? false))))

(rf/reg-event-fx
 :chats/load
 (fn [{:keys [db]} _]
   {:db (assoc db :chats/loading? true)
    ::fx/invoke {:cmd "list_chats"
                :on-success [:chats/loaded]
                :on-error [:app/set-error]}}))

(rf/reg-event-fx
 :chats/select
 (fn [{:keys [db]} [_ chat-id]]
   {:db (assoc db :chats/active-id chat-id)
    ::fx/invoke {:cmd "get_messages"
                :args {:chat_id chat-id :limit 50}
                :on-success [:messages/loaded chat-id]
                :on-error [:app/set-error]}
    ::fx/invoke {:cmd "get_conversation_memory"
                :args {:chat_id chat-id}
                :on-success [:memory/loaded chat-id]
                :on-error [:app/set-error]}}))

(rf/reg-event-db
 :chats/set-search
 (fn [db [_ q]]
   (assoc db :chats/search-query q)))

(rf/reg-event-db
 :chats/sync
 (fn [db [_ chats]]
   (assoc db :chats/list chats)))

(rf/reg-event-db
 :chats/pin
 (fn [db [_ chat-id pinned]]
   (update db :chats/list
           (fn [cs]
             (mapv #(if (= (:id %) chat-id) (assoc % :isPinned pinned) %) cs)))))

(rf/reg-event-db
 :chats/archive
 (fn [db [_ chat-id archived]]
   (update db :chats/list
           (fn [cs]
             (mapv #(if (= (:id %) chat-id) (assoc % :isArchived archived) %) cs)))))

(rf/reg-event-db
 :chats/set-mode
 (fn [db [_ chat-id mode]]
   (update db :chats/list
           (fn [cs]
             (mapv #(if (= (:id %) chat-id) (assoc % :conversationMode mode) %) cs)))))

;; Messages
(rf/reg-event-db
 :messages/loaded
 (fn [db [_ chat-id messages]]
   (-> db (assoc-in [:messages/by-chat chat-id] messages)
       (assoc :messages/loading? false))))

(rf/reg-event-db
 :messages/received
 (fn [db [_ msg]]
   (let [chat-id (:chatId msg)]
     (update-in db [:messages/by-chat chat-id]
                (fn [msgs] (conj (vec msgs) msg))))))

(rf/reg-event-fx
 :messages/send-text
 (fn [{:keys [db]} [_ chat-id text]]
   {:db (assoc db :ui/compose-text "")
    ::fx/invoke {:cmd "send_text_message"
                :args {:chat_id chat-id :text text}
                :on-success [:messages/sent]
                :on-error [:app/set-error]}}))

(rf/reg-event-db
 :messages/sent
 (fn [db _] db))

;; Contacts
(rf/reg-event-db
 :contacts/loaded
 (fn [db [_ contacts]]
   (-> db (assoc :contacts/list contacts) (assoc :contacts/loading? false))))

(rf/reg-event-fx
 :contacts/load
 (fn [_ _]
   {::fx/invoke {:cmd "list_contacts"
               :on-success [:contacts/loaded]
               :on-error [:app/set-error]}}))

;; AI
(rf/reg-event-db
 :ai/providers-loaded
 (fn [db [_ providers]]
   (assoc db :ai/providers providers)))

(rf/reg-event-db
 :ai/set-active-provider
 (fn [db [_ id]]
   (assoc db :ai/active-provider id)))

(rf/reg-event-db
 :ai/add-user-message
 (fn [db [_ content]]
   (update db :ai/conversation conj {:role "user" :content content})))

(rf/reg-event-db
 :ai/stream-chunk
 (fn [db [_ chunk]]
   (-> db (update :ai/streaming-content str chunk))))

(rf/reg-event-db
 :ai/stream-done
 (fn [db [_ content]]
   (-> db (assoc :ai/streaming? false)
       (assoc :ai/streaming-content "")
       (update :ai/conversation conj {:role "assistant" :content content}))))

(rf/reg-event-fx
 :ai/send-message
 (fn [{:keys [db]} [_ text]]
   (let [provider-id (:ai/active-provider db)
         history (:ai/conversation db)
         new-msg {:role "user" :content text}
         messages (conj history new-msg)
         provider (first (filter #(= (:id %) provider-id) (:ai/providers db)))
         streaming? (and provider (:streamingEnabled provider))]
     {:db (-> db (update :ai/conversation conj new-msg)
              (assoc :ai/loading? true)
              (assoc :ai/streaming? streaming?)
              (assoc :ai/streaming-content ""))
      ::fx/invoke {:cmd "send_ai_message"
                  :args {:provider_id provider-id
                         :messages messages
                         :stream streaming?}
                  :on-success (if streaming? [:ai/stream-start] [:ai/response-received])
                  :on-error [:ai/error]}})))

(rf/reg-event-db
 :ai/stream-start
 (fn [db _]
   (assoc db :ai/loading? false)))

(rf/reg-event-db
 :ai/response-received
 (fn [db [_ response]]
   (-> db (assoc :ai/loading? false)
       (assoc :ai/streaming? false)
       (update :ai/conversation conj {:role "assistant"
                                      :content (:content response)}))))

(rf/reg-event-db
 :ai/error
 (fn [db [_ err]]
   (-> db (assoc :ai/loading? false)
       (assoc :ai/streaming? false)
       (assoc :app/error (str "AI error: " err)))))

(rf/reg-event-db
 :ai/clear-conversation
 (fn [db _]
   (assoc db :ai/conversation [])))

(rf/reg-event-db
 :ai/templates-loaded
 (fn [db [_ templates]]
   (assoc db :ai/prompt-templates templates)))

(rf/reg-event-db
 :ai/select-template
 (fn [db [_ template]]
   (assoc db :ai/selected-template template)))

(rf/reg-event-db
 :ai/set-panel
 (fn [db [_ panel]]
   (assoc db :ui/ai-panel panel)))

;; Memory
(rf/reg-event-db
 :memory/loaded
 (fn [db [_ chat-id memory]]
   (assoc-in db [:memory/data chat-id] memory)))

(rf/reg-event-db
 :memory/update
 (fn [db [_ chat-id memory]]
   (assoc-in db [:memory/data chat-id] memory)))

;; Settings
(rf/reg-event-db
 :settings/loaded
 (fn [db [_ settings]]
   (assoc db :settings/data settings)))

(rf/reg-event-fx
 :settings/save
 (fn [{:keys [db]} [_ settings]]
   {:db (assoc db :settings/data settings)
    ::fx/invoke {:cmd "save_settings"
                :args {:settings settings}
                :on-success [:settings/saved]
                :on-error [:app/set-error]}}))

(rf/reg-event-db
 :settings/saved
 (fn [db _] db))

;; UI
(rf/reg-event-db
 :ui/set-compose-text
 (fn [db [_ text]]
   (assoc db :ui/compose-text text)))

(rf/reg-event-db
 :ui/toggle-memory
 (fn [db _]
   (update db :memory/visible? not)))

(rf/reg-event-db
 :ui/toggle-translation
 (fn [db _]
   (update db :translation/translate-before-send? not)))

(rf/reg-event-db
 :ui/open-modal
 (fn [db [_ content]]
   (-> db (assoc :ui/modal-open? true) (assoc :ui/modal-content content))))

(rf/reg-event-db
 :ui/close-modal
 (fn [db _]
   (-> db (assoc :ui/modal-open? false) (assoc :ui/modal-content nil))))

;; Automation
(rf/reg-event-db
 :automation/set-tab
 (fn [db [_ tab]]
   (assoc db :ui/automation-tab tab)))

(rf/reg-event-db
 :rules/loaded
 (fn [db [_ rules]]
   (assoc db :rules/list rules)))

(rf/reg-event-db
 :rules/edit
 (fn [db [_ rule]]
   (-> db (assoc :rules/editing rule) (assoc :rules/editor-open? true))))

(rf/reg-event-db
 :rules/close-editor
 (fn [db _]
   (assoc db :rules/editor-open? false)))

(rf/reg-event-db
 :scheduler/loaded
 (fn [db [_ tasks]]
   (assoc db :scheduler/tasks tasks)))

(rf/reg-event-db
 :plugins/loaded
 (fn [db [_ plugins]]
   (assoc db :plugins/list plugins)))

(rf/reg-event-db
 :leads/loaded
 (fn [db [_ results]]
   (assoc db :leads/results results)))

(rf/reg-event-db
 :voice/set-listening
 (fn [db [_ listening?]]
   (assoc db :voice/listening? listening?)))

;; Notifications
(rf/reg-event-db
 :ui/push-notification
 (fn [db [_ notification]]
   (update db :ui/notifications conj notification)))

(rf/reg-event-db
 :ui/dismiss-notification
 (fn [db [_ id]]
   (update db :ui/notifications (fn [ns] (remove #(= (:id %) id) ns)))))
