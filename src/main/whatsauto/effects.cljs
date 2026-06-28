(ns whatsauto.effects
  "re-frame effect handlers.
   Effects are the ONLY place where side effects happen.
   Each effect is a pure description; the handler executes it."
  (:require
   [re-frame.core :as rf]
   [whatsauto.tauri :as tauri]))

;; ============================================================
;; Tauri command invocation
;; ============================================================

(rf/reg-fx
 ::invoke-command
 (fn [{:keys [cmd args on-success on-error]}]
   (let [js-args (clj->js (or args {}))]
     (-> (tauri/invoke cmd js-args)
         (.then (fn [result]
                  (let [clj-result (js->clj result :keywordize-keys true)]
                    (rf/dispatch (into on-success [clj-result])))))
         (.catch (fn [err]
                   (let [msg (or (.-message err) (str err))]
                     (rf/dispatch (into on-error [msg])))))))))

;; ============================================================
;; Settings shortcuts
;; ============================================================

(rf/reg-fx
 ::load-settings
 (fn [_]
   (rf/dispatch [:settings/load])))

(rf/reg-fx
 ::load-chats
 (fn [_]
   (rf/dispatch [:chats/load])))

(rf/reg-fx
 ::load-ai-providers
 (fn [_]
   (rf/dispatch [:ai/load-providers])))

;; ============================================================
;; Tauri event listener registration
;; ============================================================

(rf/reg-fx
 ::listen-tauri-event
 (fn [{:keys [event-name dispatch]}]
   (tauri/listen event-name
                 (fn [payload]
                   (let [data (js->clj (.-payload payload) :keywordize-keys true)]
                     (rf/dispatch (into dispatch [data])))))))

;; ============================================================
;; DOM focus
;; ============================================================

(rf/reg-fx
 ::focus-element
 (fn [id]
   (when-let [el (.getElementById js/document id)]
     (.focus el))))
