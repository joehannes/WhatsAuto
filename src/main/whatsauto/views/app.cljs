(ns whatsauto.views.app
  "Root application layout.
   Composes the sidebar navigation with the active view.
   Handles theme application and initial data loading."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.views.sidebar :as sidebar]
   [whatsauto.views.chats :as chats]
   [whatsauto.views.ai :as ai]
   [whatsauto.views.contacts :as contacts]
   [whatsauto.views.settings :as settings]
   [whatsauto.views.qr-login :as qr-login]))

;; ============================================================
;; View routing
;; ============================================================

(defn- active-view [view wa-status]
  (cond
    ;; WhatsApp not connected: show QR / connect screen
    (and (not= :connected wa-status)
         (#{:chats :contacts} view))
    [qr-login/panel]

    :else
    (case view
      :chats    [chats/panel]
      :ai       [ai/panel]
      :contacts [contacts/panel]
      :settings [settings/panel]
      ;; Placeholder for future views
      [:div.flex.items-center.justify-center.flex-1.text-muted-foreground
       [:p.text-sm (str "View '" (name view) "' coming soon")]])))

;; ============================================================
;; Root component
;; ============================================================

(defn root []
  (r/with-let
    [active-view-sub (rf/subscribe [:nav/active-view])
     wa-status-sub   (rf/subscribe [:wa/status])
     theme-sub       (rf/subscribe [:settings/theme])
     error-sub       (rf/subscribe [:app/error])]

    ;; Apply theme to document root
    (let [theme @theme-sub]
      (-> js/document .-documentElement .-classList
          (.remove "dark" "light"))
      (case theme
        "dark"  (-> js/document .-documentElement .-classList (.add "dark"))
        "light" (-> js/document .-documentElement .-classList (.add "light"))
        ;; system: let CSS media query handle it
        nil))

    [:div.flex.h-screen.w-screen.overflow-hidden
     {:class "bg-background"}

     ;; Global toast container
     [ui/toaster {:position "bottom-right" :theme "dark" :richColors true}]

     ;; Sidebar navigation
     [ui/sidebar-provider
      [sidebar/nav-sidebar]

      ;; Main content area
      [ui/sidebar-inset
       {:class "flex flex-col overflow-hidden"}

       ;; Error banner
       (when-let [err @error-sub]
         [:div.bg-destructive.text-destructive-foreground.px-4.py-2.text-sm.flex.items-center.justify-between
          [:span err]
          [:button.hover:opacity-75
           {:on-click #(rf/dispatch [:app/set-error nil])}
           "×"]])

       ;; Active view
       [:div.flex.flex-1.overflow-hidden
        [active-view @active-view-sub @wa-status-sub]]]]]))
