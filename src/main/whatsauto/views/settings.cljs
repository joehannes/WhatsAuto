(ns whatsauto.views.settings
  "Application settings view.
   Covers theme, notifications, business profile, and AI configuration."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.interop.icons :as ic]))

(defn- section-header [title description]
  [:div.mb-4
   [:h3.font-semibold.text-base title]
   [:p.text-sm.text-muted-foreground description]])

(defn panel []
  (let [settings  @(rf/subscribe [:settings/data])
        !draft    (r/atom settings)]
    [:div.flex-1.overflow-auto.p-6
     [:div.max-w-2xl.mx-auto

      ;; Page header
      [:div.mb-8
       [:h2.text-2xl.font-bold "Settings"]
       [:p.text-muted-foreground "Configure WhatsAuto to your preferences"]]

      ;; Appearance
      [ui/card {:class "mb-6"}
       [ui/card-header
        [ui/card-title "Appearance"]
        [ui/card-description "Customize the look and feel"]]
       [ui/card-content {:class "space-y-4"}
        [:div.flex.items-center.justify-between
         [:div
          [:p.font-medium.text-sm "Theme"]
          [:p.text-xs.text-muted-foreground "Choose your preferred color scheme"]]
         [ui/select
          {:value     (get @!draft :theme "dark")
           :on-value-change #(swap! !draft assoc :theme %)}
          [ui/select-trigger {:class "w-36"}
           [ui/select-value]]
          [ui/select-content
           [ui/select-item {:value "dark"}  [:<> [ic/icon-moon {:size 14}] "Dark"]]
           [ui/select-item {:value "light"} [:<> [ic/icon-sun {:size 14}] "Light"]]
           [ui/select-item {:value "system"} [:<> [ic/icon-system {:size 14}] "System"]]]]]]]

      ;; Notifications
      [ui/card {:class "mb-6"}
       [ui/card-header
        [ui/card-title "Notifications"]
        [ui/card-description "Control how you receive alerts"]]
       [ui/card-content {:class "space-y-4"}
        [:div.flex.items-center.justify-between
         [:div
          [:p.font-medium.text-sm "Desktop notifications"]
          [:p.text-xs.text-muted-foreground "Show system notifications for new messages"]]
         [ui/switch {:checked   (boolean (:notifications_enabled @!draft))
                     :on-checked-change #(swap! !draft assoc :notifications_enabled %)}]]
        [ui/separator]
        [:div.flex.items-center.justify-between
         [:div
          [:p.font-medium.text-sm "Notification sounds"]
          [:p.text-xs.text-muted-foreground "Play a sound for incoming messages"]]
         [ui/switch {:checked   (boolean (:sound_enabled @!draft))
                     :on-checked-change #(swap! !draft assoc :sound_enabled %)}]]]]

      ;; AI auto-reply
      [ui/card {:class "mb-6"}
       [ui/card-header
        [ui/card-title "AI Auto-Reply"]
        [ui/card-description "Let AI automatically respond to messages"]]
       [ui/card-content
        [:div.flex.items-center.justify-between
         [:div
          [:p.font-medium.text-sm "Enable auto-reply"]
          [:p.text-xs.text-muted-foreground
           "AI will respond to messages when you are away"]]
         [ui/switch {:checked   (boolean (:auto_reply_enabled @!draft))
                     :on-checked-change #(swap! !draft assoc :auto_reply_enabled %)}]]]]

      ;; Business profile
      [ui/card {:class "mb-6"}
       [ui/card-header
        [ui/card-title "Business Profile"]
        [ui/card-description "Your business information"]]
       [ui/card-content {:class "space-y-4"}
        [:div
         [:label.text-sm.font-medium.block.mb-1.5 "Business name"]
         [:input.w-full.bg-input.rounded-lg.px-3.py-2.text-sm
          .border.border-border.outline-none
          {:class     "focus:border-ring"
           :value     (or (:business_name @!draft) "")
           :on-change #(swap! !draft assoc :business_name (-> % .-target .-value))}]]
        [:div
         [:label.text-sm.font-medium.block.mb-1.5 "Business phone"]
         [:input.w-full.bg-input.rounded-lg.px-3.py-2.text-sm
          .border.border-border.outline-none
          {:class     "focus:border-ring"
           :value     (or (:business_phone @!draft) "")
           :on-change #(swap! !draft assoc :business_phone (-> % .-target .-value))}]]]]

      ;; Save button
      [:div.flex.justify-end.gap-3
       [ui/button {:variant  "outline"
                   :on-click #(reset! !draft settings)}
        "Cancel"]
       [ui/button {:on-click #(rf/dispatch [:settings/save @!draft])}
        [ic/icon-check {:size 14}]
        "Save Settings"]]

      ;; App info footer
      [:div.mt-8.text-center.text-xs.text-muted-foreground
       [:p "WhatsAuto v0.1.0 — Open Source AI Business Messenger"]
       [:p.mt-1 "MIT License · github.com/joehannes/WhatsAuto"]]]]))
