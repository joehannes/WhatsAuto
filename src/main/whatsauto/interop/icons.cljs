(ns whatsauto.interop.icons
  "Lucide icon helpers for Reagent.
   Import individual icons to keep bundle size minimal."
  (:require
   [reagent.core :as r]
   ["lucide-react" :refer
    [MessageSquare Users Bot Settings2 LayoutDashboard
     Megaphone Zap Globe Mic BookOpen GitBranch Puzzle
     ScrollText SlidersHorizontal ChevronLeft ChevronRight
     Search Send Paperclip Smile MoreVertical Phone Video
     Archive Star Check CheckCheck X Plus Trash2 Edit
     Wifi WifiOff QrCode Moon Sun Monitor RefreshCw
     Loader2 AlertCircle Info Bell BellOff
     LogOut User Tag Hash Filter]]))

(defn- icon [component]
  (r/adapt-react-class component))

;; Navigation icons
(def icon-chats          (icon MessageSquare))
(def icon-contacts       (icon Users))
(def icon-ai             (icon Bot))
(def icon-settings       (icon Settings2))
(def icon-dashboard      (icon LayoutDashboard))
(def icon-campaigns      (icon Megaphone))
(def icon-automation     (icon Zap))
(def icon-translation    (icon Globe))
(def icon-voice          (icon Mic))
(def icon-knowledge      (icon BookOpen))
(def icon-rules          (icon GitBranch))
(def icon-plugins        (icon Puzzle))
(def icon-logs           (icon ScrollText))

;; Action icons
(def icon-search         (icon Search))
(def icon-send           (icon Send))
(def icon-attach         (icon Paperclip))
(def icon-emoji          (icon Smile))
(def icon-more           (icon MoreVertical))
(def icon-phone          (icon Phone))
(def icon-video          (icon Video))
(def icon-archive        (icon Archive))
(def icon-star           (icon Star))
(def icon-check          (icon Check))
(def icon-check-check    (icon CheckCheck))
(def icon-close          (icon X))
(def icon-plus           (icon Plus))
(def icon-delete         (icon Trash2))
(def icon-edit           (icon Edit))
(def icon-refresh        (icon RefreshCw))

;; Status icons
(def icon-connected      (icon Wifi))
(def icon-disconnected   (icon WifiOff))
(def icon-qr             (icon QrCode))
(def icon-loading        (icon Loader2))
(def icon-alert          (icon AlertCircle))
(def icon-info           (icon Info))
(def icon-bell           (icon Bell))
(def icon-bell-off       (icon BellOff))
(def icon-logout         (icon LogOut))

;; Theme icons
(def icon-moon           (icon Moon))
(def icon-sun            (icon Sun))
(def icon-system         (icon Monitor))

;; Misc
(def icon-user           (icon User))
(def icon-tag            (icon Tag))
(def icon-hash           (icon Hash))
(def icon-filter         (icon Filter))
(def icon-sliders        (icon SlidersHorizontal))
(def icon-chevron-left   (icon ChevronLeft))
(def icon-chevron-right  (icon ChevronRight))

;; Aliases used by Phase 2 views
(def icon-zap            icon-automation)
(def icon-globe          icon-translation)
(def icon-mic            icon-voice)
(def icon-puzzle         icon-plugins)
(def icon-map-pin        icon-campaigns)
