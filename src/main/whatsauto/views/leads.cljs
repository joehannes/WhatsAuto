(ns whatsauto.views.leads
  "Lead discovery view — search for business contacts."
  (:require [re-frame.core :as rf]
            [whatsauto.interop.shadcn :as ui]
            [whatsauto.interop.icons :as ic]))

(defn panel []
  [:div.flex.flex-col.flex-1.p-6.overflow-hidden
   [:div.mb-6
    [:h2.text-2xl.font-bold "Lead Discovery"]
    [:p.text-muted-foreground "Find potential business contacts"]]
   [:div.text-center.py-12.text-muted-foreground
    [ic/icon-map-pin {:size 32 :class "mb-2 opacity-30"}]
    [:p.text-sm "Lead discovery coming soon"]
    [:p.text-xs.mt-1 "Search Google Maps, directories, and the web"]]])
