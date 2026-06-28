(ns whatsauto.subs
  "re-frame subscriptions — query projections over app-db.
   Subscriptions are pure functions; they memoize automatically."
  (:require
   [re-frame.core :as rf]))

;; ============================================================
;; App state
;; ============================================================

(rf/reg-sub :app/ready?    (fn [db _] (:app/ready? db)))
(rf/reg-sub :app/loading?  (fn [db _] (:app/loading? db)))
(rf/reg-sub :app/error     (fn [db _] (:app/error db)))

;; ============================================================
;; Navigation
;; ============================================================

(rf/reg-sub :nav/active-view  (fn [db _] (:nav/active-view db)))
(rf/reg-sub :nav/active-panel (fn [db _] (:nav/active-panel db)))
(rf/reg-sub :ui/sidebar-collapsed? (fn [db _] (:ui/sidebar-collapsed? db)))

;; ============================================================
;; WhatsApp
;; ============================================================

(rf/reg-sub :wa/status       (fn [db _] (:wa/status db)))
(rf/reg-sub :wa/qr-code      (fn [db _] (:wa/qr-code db)))
(rf/reg-sub :wa/connected?   (fn [db _] (= :connected (:wa/status db))))

;; ============================================================
;; Chats
;; ============================================================

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
                 (clojure.string/lower-case (or (:name %) "")) q)
               chats)))))

(rf/reg-sub
 :chats/active
 (fn [_ _]
   [(rf/subscribe [:chats/all])
    (rf/subscribe [:chats/active-id])])
 (fn [[chats id] _]
   (when id (first (filter #(= (:id %) id) chats)))))

;; ============================================================
;; Messages
;; ============================================================

(rf/reg-sub :messages/loading? (fn [db _] (:messages/loading? db)))

(rf/reg-sub
 :messages/active-chat
 (fn [_ _]
   [(rf/subscribe [:messages/by-chat])
    (rf/subscribe [:chats/active-id])])
 (fn [[by-chat id] _]
   (when id (get by-chat id []))))

(rf/reg-sub
 :messages/by-chat
 (fn [db _] (:messages/by-chat db)))

;; ============================================================
;; Contacts
;; ============================================================

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

;; ============================================================
;; AI
;; ============================================================

(rf/reg-sub :ai/providers        (fn [db _] (:ai/providers db)))
(rf/reg-sub :ai/active-provider  (fn [db _] (:ai/active-provider db)))
(rf/reg-sub :ai/conversation     (fn [db _] (:ai/conversation db)))
(rf/reg-sub :ai/loading?         (fn [db _] (:ai/loading? db)))
(rf/reg-sub :ai/config-open?     (fn [db _] (:ai/config-open? db)))

(rf/reg-sub
 :ai/active-provider-data
 (fn [_ _]
   [(rf/subscribe [:ai/providers])
    (rf/subscribe [:ai/active-provider])])
 (fn [[providers id] _]
   (when id (first (filter #(= (:id %) id) providers)))))

;; ============================================================
;; Settings
;; ============================================================

(rf/reg-sub :settings/data  (fn [db _] (:settings/data db)))
(rf/reg-sub :settings/theme (fn [db _] (get-in db [:settings/data :theme] "dark")))

;; ============================================================
;; UI
;; ============================================================

(rf/reg-sub :ui/compose-text    (fn [db _] (:ui/compose-text db)))
(rf/reg-sub :ui/typing-chats    (fn [db _] (:ui/typing-chats db)))
(rf/reg-sub :ui/notifications   (fn [db _] (:ui/notifications db)))
