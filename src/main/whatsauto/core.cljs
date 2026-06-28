(ns whatsauto.core
  "Application entry point.
   Mounts the root Reagent component and initialises re-frame."
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [whatsauto.effects]
   [whatsauto.events]
   [whatsauto.subs]
   [whatsauto.tauri :as tauri]
   [whatsauto.views.app :as app]))

(defn mount-app []
  (rdom/render [app/root]
               (.getElementById js/document "app")))

(defn ^:dev/after-load hot-reload []
  (rf/clear-subscription-cache!)
  (mount-app))

(defn init []
  (rf/dispatch-sync [:app/init])
  (tauri/listen-all)
  (rf/dispatch [:app/ready])
  (mount-app))
