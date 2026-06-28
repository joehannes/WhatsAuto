(ns whatsauto.views.qrlogin
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
     [:div.flex.flex-col.items-center.gap-4
      [ic/icon-chats {:size 36 :class "text-primary"}]
      [:h1.text-2xl.font-bold "WhatsAuto"]]
     [ui/card {:class "w-full max-w-md"}
      [ui/card-header
       [ui/card-title (name (or status :disconnected))]]
      [ui/card-content
       [:div.flex.flex-col.items-center.gap-6
        (when qr-code
          [:img {:src (str "data:image/png;base64," qr-code)
                 :alt "WhatsApp QR Code"
                 :class "size-52"}])
        (when (= status :disconnected)
          [ui/button {:on-click #(rf/dispatch [:wa/connect])}
           "Connect to WhatsApp"])
        (when (= status :qr)
          [ui/button {:variant "outline"
                      :on-click #(rf/dispatch [:wa/refresh-qr])}
           "Refresh QR"])]]]]))
