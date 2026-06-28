(ns whatsauto.subs
  "re-frame subscriptions — pure query projections over app-db."
  (:require [re-frame.core :as rf]))

;; App
(rf/reg-sub :app/ready?    (fn [db _] (:app/ready? db)))
(rf/reg-sub :app/loading?  (fn [db _] (:app/loading? db)))
(rf/reg-sub :app/error     (fn [db _] (:app/error db)))

;; Nav
(rf/reg-sub :nav/active-view  (fn [db _] (:nav/active-view db)))
(rf/reg-sub :ui/sidebar-collapsed? (fn [db _] (:ui/sidebar-collapsed? db)))

;; WhatsApp
(rf/reg-sub :wa/status       (fn [db _] (:wa/status db)))
(rf/reg-sub :wa/qr-code      (fn [db _] (:wa/qr-code db)))
(rf/reg-sub :wa/connected?   (fn [db _] (= :connected (:wa/status db))))
(rf/reg-sub :wa/typing?       (fn [db [_ chat-id]] (contains? (:wa/typing-chats db) chat-id)))

;; Chats
(rf/reg-sub :chats/all      (fn [db _] (:chats/list db)))
(rf/reg-sub :chats/loading? (fn [db _] (:chats/loading? db)))
(rf/reg-sub :chats/active-id (fn [db _] (:chats/active-id db)))
(rf/reg-sub :chats/search-query (fn [db _] (:chats/search-query db)))

(rf/reg-sub
 :chats/filtered
 (fn [_ _]
   [(rf/subscribe [:chats/all])
    (rf/subscribe [:chats/search-query])])
 (fn [[chats query] _]
   (if (empty? query)
     chats
     (let [q (clojure.string/lower-case query)]
       (filter #(clojure.string/includes?
                 (clojure.string/lower-case (or (:name %) (:jid %) "")) q)
               chats)))))

(rf/reg-sub
 :chats/pinned
 (fn [_ _]
   [(rf/subscribe [:chats/all])])
 (fn [[chats] _]
   (filter :isPinned chats)))

(rf/reg-sub
 :chats/active
 (fn [_ _]
   [(rf/subscribe [:chats/all])
    (rf/subscribe [:chats/active-id])])
 (fn [[chats id] _]
   (when id (first (filter #(= (:id %) id) chats)))))

;; Messages
(rf/reg-sub :messages/loading? (fn [db _] (:messages/loading? db)))
(rf/reg-sub :messages/by-chat (fn [db _] (:messages/by-chat db)))

(rf/reg-sub
 :messages/active-chat
 (fn [_ _]
   [(rf/subscribe [:messages/by-chat])
    (rf/subscribe [:chats/active-id])])
 (fn [[by-chat id] _]
   (when id (get by-chat id []))))

;; Contacts
(rf/reg-sub :contacts/all      (fn [db _] (:contacts/list db)))
(rf/reg-sub :contacts/loading? (fn [db _] (:contacts/loading? db)))
(rf/reg-sub :contacts/search   (fn [db _] (:contacts/search db)))

(rf/reg-sub
 :contacts/filtered
 (fn [_ _]
   [(rf/subscribe [:contacts/all])
    (rf/subscribe [:contacts/search])])
 (fn [[contacts q] _]
   (if (empty? q)
     contacts
     (let [lq (clojure.string/lower-case q)]
       (filter #(clojure.string/includes?
                 (clojure.string/lower-case (or (:name %) "")) lq)
               contacts)))))

;; AI
(rf/reg-sub :ai/providers        (fn [db _] (:ai/providers db)))
(rf/reg-sub :ai/active-provider  (fn [db _] (:ai/active-provider db)))
(rf/reg-sub :ai/conversation     (fn [db _] (:ai/conversation db)))
(rf/reg-sub :ai/loading?         (fn [db _] (:ai/loading? db)))
(rf/reg-sub :ai/streaming?       (fn [db _] (:ai/streaming? db)))
(rf/reg-sub :ai/streaming-content (fn [db _] (:ai/streaming-content db)))
(rf/reg-sub :ai/prompt-templates (fn [db _] (:ai/prompt-templates db)))
(rf/reg-sub :ai/selected-template (fn [db _] (:ai/selected-template db)))
(rf/reg-sub :ai/panel            (fn [db _] (:ui/ai-panel db)))

(rf/reg-sub
 :ai/active-provider-data
 (fn [_ _]
   [(rf/subscribe [:ai/providers])
    (rf/subscribe [:ai/active-provider])])
 (fn [[providers id] _]
   (when id (first (filter #(= (:id %) id) providers)))))

;; Memory
(rf/reg-sub :memory/data (fn [db _] (:memory/data db)))
(rf/reg-sub :memory/visible? (fn [db _] (:memory/visible? db)))

(rf/reg-sub
 :memory/active-chat
 (fn [_ _]
   [(rf/subscribe [:memory/data])
    (rf/subscribe [:chats/active-id])])
 (fn [[mem id] _]
   (when id (get mem id))))

;; Settings
(rf/reg-sub :settings/data  (fn [db _] (:settings/data db)))
(rf/reg-sub :settings/theme (fn [db _] (get-in db [:settings/data :theme] "system")))

;; Automation
(rf/reg-sub :automation/tab (fn [db _] (:ui/automation-tab db)))
(rf/reg-sub :rules/list (fn [db _] (:rules/list db)))
(rf/reg-sub :rules/editor-open? (fn [db _] (:rules/editor-open? db)))
(rf/reg-sub :rules/editing (fn [db _] (:rules/editing db)))
(rf/reg-sub :scheduler/tasks (fn [db _] (:scheduler/tasks db)))
(rf/reg-sub :plugins/list (fn [db _] (:plugins/list db)))
(rf/reg-sub :leads/results (fn [db _] (:leads/results db)))

;; UI
(rf/reg-sub :ui/compose-text    (fn [db _] (:ui/compose-text db)))
(rf/reg-sub :ui/notifications   (fn [db _] (:ui/notifications db)))
(rf/reg-sub :ui/modal-open?     (fn [db _] (:ui/modal-open? db)))
(rf/reg-sub :ui/modal-content   (fn [db _] (:ui/modal-content db)))
(rf/reg-sub :ui/translation-enabled? (fn [db _] (get-in db [:settings/data :translation-enabled] false)))
(rf/reg-sub :ui/voice-available? (fn [db _] (:voice/available? db)))
(rf/reg-sub :ui/voice-listening? (fn [db _] (:voice/listening? db)))
