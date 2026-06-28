(ns whatsauto.views.voice
  "Voice view — STT/TTS configuration, voice input controls."
  (:require [re-frame.core :as rf]
            [whatsauto.interop.shadcn :as ui]
            [whatsauto.interop.icons :as ic]))

(defn panel []
  (let [enabled @(rf/subscribe [:ui/voice-available?])]
    [:div.flex.flex-col.flex-1.p-6.overflow-hidden
     [:div.mb-6
      [:h2.text-2xl.font-bold "Voice"]
      [:p.text-muted-foreground "Speech-to-text and text-to-speech"]]
     [:div.text-center.py-12.text-muted-foreground
      [ic/icon-mic {:size 32 :class "mb-2 opacity-30"}]
      [:p.text-sm "Voice features coming soon"]
      [:p.text-xs.mt-1 "Configure microphone and speech providers"]]]))
