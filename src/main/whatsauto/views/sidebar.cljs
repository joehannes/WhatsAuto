(ns whatsauto.views.sidebar
  "Navigation sidebar with all Phase 2 views.
   Shows app navigation items with icons and badges."
  (:require [re-frame.core :as rf]
            [whatsauto.interop.shadcn :as ui]
            [whatsauto.interop.icons :as ic]))

(def ^:private nav-items
  [{:id :chats    :label "Chats"        :icon ic/icon-chats}
   {:id :contacts :label "Contacts"     :icon ic/icon-contacts}
   {:id :ai       :label "AI Assistant" :icon ic/icon-ai}
   {:separator true}
   {:id :automation :label "Automation"  :icon ic/icon-automation}
   {:id :translation :label "Translation" :icon ic/icon-translation}
   {:id :voice    :label "Voice"         :icon ic/icon-voice}
   {:id :lead-discovery :label "Leads"     :icon ic/icon-campaigns}
   {:separator true}
   {:id :plugins    :label "Plugins"     :icon ic/icon-plugins}
   {:id :logs       :label "Logs"        :icon ic/icon-logs}
   {:separator true}
   {:id :settings   :label "Settings"    :icon ic/icon-settings}])

(defn- status-indicator []
  (let [status @(rf/subscribe [:wa/status])]
    [:div.flex.items-center.gap-2.px-2.py-1.text-xs
     (case status
       :connected
       [:<>
        [:div.size-2.rounded-full.bg-wa-green.ai-pulse]
        [:span.text-muted-foreground "Connected"]]
       :qr
       [:<>
        [:div.size-2.rounded-full {:class "bg-yellow-500 animate-pulse"}]
        [:span.text-muted-foreground "Scan QR"]]
       [:<>
        [:div.size-2.rounded-full.bg-destructive]
        [:span.text-muted-foreground "Disconnected"]])]))

(defn- unread-badge []
  (let [chats   @(rf/subscribe [:chats/all])
        unread  (->> chats (map :unreadCount) (filter pos?) count)]
    (when (pos? unread)
      [ui/sidebar-menu-badge (if (> unread 99) "99+" (str unread))])))

(defn- sidebar-logo []
  [:div.flex.items-center.gap-3.px-4.py-3
   [:div.size-8.rounded-full.flex.items-center.justify-center
    {:class "bg-primary"}
    [ic/icon-chats {:size 16 :class "text-primary-foreground"}]]
   [:div
    [:p.font-semibold.text-sm.text-foreground "WhatsAuto"]
    [:p.text-xs.text-muted-foreground "Business Messenger"]]])

(defn- nav-item [{:keys [id label icon]}]
  (let [active-view @(rf/subscribe [:nav/active-view])
        active?     (= id active-view)]
    [ui/sidebar-menu-item
     [ui/sidebar-menu-button
      {:is-active active?
       :on-click  #(rf/dispatch [:nav/set-view id])
       :tooltip   label
       :class     (when active? "text-primary font-medium")}
      [icon {:size 18}]
      [:span label]
      (when (= id :chats) [unread-badge])]]))

(defn nav-sidebar []
  [ui/sidebar
   {:collapsible "icon" :class "border-r border-sidebar-border"}
   [ui/sidebar-header
    [sidebar-logo]
    [status-indicator]]
   [ui/sidebar-separator]
   [ui/sidebar-content
    [ui/sidebar-group
     [ui/sidebar-group-label "Messenger"]
     [ui/sidebar-group-content
      [ui/sidebar-menu
       (for [{:keys [id separator] :as item} nav-items]
         (if separator
           ^{:key (str "sep-" (rand-int 10000))}
           [ui/sidebar-separator {:class "my-1"}]
           ^{:key (str id)}
           [nav-item item]))]]]]
   [ui/sidebar-footer
    [ui/sidebar-separator]
    [:div.p-2
     [ui/sidebar-menu
      [ui/sidebar-menu-item
       [ui/sidebar-menu-button
        {:on-click #(rf/dispatch [:nav/set-view :settings])
         :tooltip  "Settings"}
        [ic/icon-settings {:size 18}]
        [:span "Settings"]]]]]]])
