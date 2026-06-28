(ns whatsauto.views.plugins
  "Plugin SDK view — manage and configure plugins."
  (:require [re-frame.core :as rf]
            [whatsauto.interop.shadcn :as ui]
            [whatsauto.interop.icons :as ic]))

(defn- plugin-card [plugin]
  [:div.p-3.rounded-lg.border.border-border.bg-card.mb-2
   [:div.flex.items-center.justify-between
    [:div.flex.items-center.gap-2
     [ic/icon-puzzle {:size 14 :class "text-primary"}]
     [:span.text-sm.font-medium (:name plugin)]]
    [ui/switch {:checked (:enabled plugin)
                :on-checked-change #(rf/dispatch [:plugins/toggle-enabled (:id plugin)])}]]
   [:p.text-xs.text-muted-foreground (:version plugin)]])

(defn panel []
  (let [plugins @(rf/subscribe [:plugins/list])]
    [:div.flex.flex-col.flex-1.p-6.overflow-hidden
     [:div.mb-6
      [:h2.text-2xl.font-bold "Plugins"]
      [:p.text-muted-foreground "Extend WhatsAuto with custom plugins"]]
     [ui/scroll-area {:class "flex-1"}
      (if (empty? plugins)
        [:div.text-center.py-12.text-muted-foreground
         [ic/icon-puzzle {:size 32 :class "mb-2 opacity-30"}]
         [:p.text-sm "No plugins installed"]
         [:p.text-xs.mt-1 "Install plugins from the SDK"]]
        [:div.flex.flex-col.gap-2
         (for [p plugins]
           ^{:key (:id p)}
           [plugin-card p])])]]))
