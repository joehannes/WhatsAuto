(ns whatsauto.views.qr-login
  "QR code login screen shown when WhatsApp is not connected."
  (:require
   [re-frame.core :as rf]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.interop.icons :as ic]))

(defn panel []
  (let [status  @(rf/subscribe [:wa/status])
        qr-code @(rf/subscribe [:wa/qr-code])]
    [:div.flex.flex-1.flex-col.items-center.justify-center.gap-8.p-8
     {:class "bg-background"}

     ;; Logo
     [:div.flex.flex-col.items-center.gap-4
      [:div.size-20.rounded-full.flex.items-center.justify-center
       {:class "bg-primary/10 border border-primary/30"}
       [ic/icon-chats {:size 36 :class "text-primary"}]]
      [:div.text-center
       [:h1.text-2xl.font-bold.text-foreground "WhatsAuto"]
       [:p.text-muted-foreground.text-sm "AI-powered Business Messenger"]]]

     ;; Status card
     [ui/card {:class "w-full max-w-md"}
      [ui/card-header
       [ui/card-title
        (case status
          :qr     "Scan QR Code"
          :disconnected "Connect WhatsApp"
          "Connecting...")]
       [ui/card-description
        (case status
          :qr     "Open WhatsApp on your phone → Settings → Linked Devices → Link a Device"
          :disconnected "WhatsApp is not connected. Click connect to get started."
          "Please wait while connecting to WhatsApp Web...")]]

      [ui/card-content
       [:div.flex.flex-col.items-center.gap-6

        ;; QR code display
        (cond
          (= status :qr)
          [:div.rounded-xl.overflow-hidden.border.border-border.bg-white.p-4
           (if qr-code
             ;; QR is base64 image from whatsapp-web.js
             [:img {:src (str "data:image/png;base64," qr-code)
                    :alt "WhatsApp QR Code"
                    :class "size-52"}]
             [:div.size-52.flex.items-center.justify-center
              [ic/icon-loading {:size 32 :class "text-primary animate-spin"}]])]

          (= status :disconnected)
          [:div.size-52.rounded-xl.border.border-dashed.border-border
           .flex.flex-col.items-center.justify-center.gap-3.text-muted-foreground
           [ic/icon-qr {:size 48 :class "opacity-30"}]
           [:p.text-sm "QR code will appear here"]]

          :else
          [:div.size-52.flex.items-center.justify-center
           [ic/icon-loading {:size 32 :class "text-primary animate-spin"}]])

        ;; Connect button
        (when (= status :disconnected)
          [ui/button
           {:on-click #(rf/dispatch [:wa/connect])
            :class    "w-full"}
           [ic/icon-connected {:size 16}]
           "Connect to WhatsApp"])

        ;; Refresh button while showing QR
        (when (= status :qr)
          [ui/button
           {:variant  "outline"
            :on-click #(rf/dispatch [:wa/refresh-qr])
            :class    "w-full"}
           [ic/icon-refresh {:size 16}]
           "Refresh QR"])]]

      ;; Footer hint
      [ui/card-footer
       {:class "flex-col gap-1 text-center"}
       [:p.text-xs.text-muted-foreground
        "Your session is stored locally and encrypted."]
       [:p.text-xs.text-muted-foreground
        "WhatsAuto never sends your messages to third-party servers."]]]]]
    ))
