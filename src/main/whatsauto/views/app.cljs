(ns whatsauto.views.app
  "Root application layout with all Phase 2 views.
   Composes sidebar navigation with the active view."
  (:require
   [re-frame.core :as rf]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.views.sidebar :as sidebar]
   [whatsauto.views.chats :as chats]
   [whatsauto.views.ai :as ai]
   [whatsauto.views.contacts :as contacts]
   [whatsauto.views.settings :as settings]
   [whatsauto.views.qr-login :as qr-login]
   [whatsauto.views.automation :as automation]
   [whatsauto.views.translation :as translation]
   [whatsauto.views.voice :as voice]
   [whatsauto.views.leads :as leads]
   [whatsauto.views.plugins :as plugins]))

(defn- active-view [view wa-status]
  (cond
    (and (not= :connected wa-status)
         (#{:chats :contacts} view))
    [qr-login/panel]
    :else
    (case view
      :chats    [chats/panel]
      :ai       [ai/panel]
      :contacts [contacts/panel]
      :settings [settings/panel]
      :automation [automation/panel]
      :translation [translation/panel]
      :voice    [voice/panel]
      :lead-discovery [leads/panel]
      :plugins  [plugins/panel]
      [:div.flex.items-center.justify-center.flex-1.text-muted-foreground
       [:p.text-sm (str "View '" (name view) "' coming soon")]])))

(defn root []
  (let [active-view-sub (rf/subscribe [:nav/active-view])
        wa-status-sub   (rf/subscribe [:wa/status])
        theme-sub       (rf/subscribe [:settings/theme])
        error-sub       (rf/subscribe [:app/error])]
    (let [theme @theme-sub]
      (-> js/document .-documentElement .-classList (.remove "dark" "light"))
      (case theme
        "dark"  (-> js/document .-documentElement .-classList (.add "dark"))
        "light" (-> js/document .-documentElement .-classList (.add "light"))
        nil))
    [:div.flex.h-screen.w-screen.overflow-hidden
     {:class "bg-background"}
     [ui/toaster {:position "bottom-right" :theme "dark" :richColors true}]
     [ui/sidebar-provider
      [sidebar/nav-sidebar]
      [ui/sidebar-inset
       {:class "flex flex-col overflow-hidden"}
       (when-let [err @error-sub]
         [:div.bg-destructive.text-destructive-foreground.px-4.py-2.text-sm.flex.items-center.justify-between
          [:span err]
          [:button.hover:opacity-75
           {:on-click #(rf/dispatch [:app/set-error nil])}
           "D"])
       [:div.flex.flex-1.overflow-hidden
        [active-view @active-view-sub @wa-status-sub]]]]]))
