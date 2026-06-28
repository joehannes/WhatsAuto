(ns whatsauto.views.translation
  "Translation view — manage profiles, enable auto-translation."
  (:require [re-frame.core :as rf]
            [whatsauto.interop.shadcn :as ui]
            [whatsauto.interop.icons :as ic]))

(defn panel []
  (let [enabled @(rf/subscribe [:translation/enabled?])
        translate-before-send @(rf/subscribe [:translation/translate-before-send?])]
    [:div.flex.flex-col.flex-1.p-6.overflow-hidden
     [:div.mb-6
      [:h2.text-2xl.font-bold "Translation"]
      [:p.text-muted-foreground "Automatic multilingual messaging"]]
     [ui/card {:class "mb-6"}
      [ui/card-content {:class "flex items-center justify-between py-4"}
       [:div
        [:p.font-medium "Enable Auto-Translation"]
        [:p.text-xs.text-muted-foreground "Translate incoming and outgoing messages"]]
       [ui/switch {:checked enabled
                   :on-checked-change #(rf/dispatch [:translation/toggle-enabled])}]]]
     [ui/card {:class "mb-6"}
      [ui/card-content {:class "flex items-center justify-between py-4"}
       [:div
        [:p.font-medium "Translate Before Sending"]
        [:p.text-xs.text-muted-foreground "Review translated message before sending"]]
       [ui/switch {:checked translate-before-send
                   :on-checked-change #(rf/dispatch [:translation/toggle-before-send])}]]]
     [:div.text-center.py-12.text-muted-foreground
      [ic/icon-globe {:size 32 :class "mb-2 opacity-30"}]
      [:p.text-sm "Translation profiles coming soon"]
      [:p.text-xs.mt-1 "Configure per-contact language settings"]]]))
