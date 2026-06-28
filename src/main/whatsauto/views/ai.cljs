(ns whatsauto.views.ai
  "AI Assistant view — second primary view of WhatsAuto.

   Two panes:
   LEFT:  Chat-style conversation with the configured AI provider.
          Shows messages, streaming indicator, clear/history controls.
   RIGHT: Provider configuration panel — API keys, model selection,
          system prompt, temperature, per-conversation mode.

   Design goals:
   - Clean, distraction-free interface
   - Live AI-is-thinking indicator
   - Easy provider switching
   - Prompt template library (foundation laid)"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.interop.icons :as ic]))

;; ============================================================
;; Provider kind metadata
;; ============================================================

(def ^:private provider-info
  {"qwen"       {:label "Qwen (Alibaba)"   :color "text-orange-400"}
   "openai"     {:label "OpenAI GPT"       :color "text-green-400"}
   "claude"     {:label "Anthropic Claude" :color "text-amber-400"}
   "gemini"     {:label "Google Gemini"    :color "text-blue-400"}
   "deepseek"   {:label "DeepSeek"         :color "text-cyan-400"}
   "ollama"     {:label "Ollama (local)"   :color "text-purple-400"}
   "lmstudio"   {:label "LM Studio"        :color "text-indigo-400"}
   "openrouter" {:label "OpenRouter"       :color "text-pink-400"}
   "custom"     {:label "Custom"           :color "text-gray-400"}})

(defn- provider-label [kind]
  (get-in provider-info [kind :label] kind))

;; ============================================================
;; Conversation message bubble
;; ============================================================

(defn- ai-bubble [{:keys [role content]}]
  (let [is-user? (= role "user")]
    [:div.flex.mb-4
     {:class (if is-user? "justify-end" "justify-start")}
     [:div.max-w-xl
      ;; Role label
      [:div.flex.items-center.gap-2.mb-1
       {:class (if is-user? "justify-end" "justify-start")}
       (if is-user?
         [:<>
          [:span.text-xs.text-muted-foreground "You"]
          [:div.size-5.rounded-full.bg-primary.flex.items-center.justify-center
           [ic/icon-user {:size 10 :class "text-primary-foreground"}]]]
         [:<>
          [:div.size-5.rounded-full.flex.items-center.justify-center
           {:class "bg-ai-purple"}
           [ic/icon-ai {:size 10 :class "text-white"}]]
          [:span.text-xs.text-muted-foreground "AI"]])]

      ;; Content
      [:div.px-4.py-3.rounded-xl.text-sm.leading-relaxed
       {:class (if is-user?
                 "bg-primary/20 text-foreground border border-primary/30"
                 "bg-card text-foreground border border-border")}
       content]]]))

;; ============================================================
;; Thinking indicator
;; ============================================================

(defn- thinking-indicator []
  [:div.flex.justify-start.mb-4
   [:div.flex.items-center.gap-2.px-4.py-3.rounded-xl.text-sm
    {:class "bg-card border border-border"}
    [:div.size-5.rounded-full.flex.items-center.justify-center
     {:class "bg-ai-purple"}
     [ic/icon-ai {:size 10 :class "text-white ai-pulse"}]]
    [:span.text-muted-foreground "AI is thinking"]
    [:span.flex.gap-1
     (for [i (range 3)]
       ^{:key i}
       [:span.size-1.rounded-full.bg-muted-foreground.animate-bounce
        {:style {:animation-delay (str (* i 0.15) "s")}}])]]])

;; ============================================================
;; AI chat compose
;; ============================================================

(defn- ai-compose []
  (let [!text    (r/atom "")
        loading? @(rf/subscribe [:ai/loading?])
        provider @(rf/subscribe [:ai/active-provider])]
    [:div.border-t.border-border.px-4.py-3
     (when-not provider
       [:p.text-xs.text-destructive.mb-2 "Configure an AI provider in the settings panel →"])
     [:div.flex.items-end.gap-2
      [:textarea.flex-1.bg-input.rounded-xl.px-3.py-2.text-sm.text-foreground.placeholder-muted-foreground.border.border-border.outline-none.resize-none.max-h-32.min-h-9
       {:class       "focus:border-ring focus:ring-1 focus:ring-ring/50"
        :placeholder "Ask the AI anything..."
        :value       @!text
        :disabled    (or loading? (not provider))
        :rows        1
        :on-change   #(reset! !text (-> % .-target .-value))
        :on-key-down (fn [e]
                       (when (and (= 13 (.-keyCode e))
                                  (not (.-shiftKey e)))
                         (.preventDefault e)
                         (let [t (clojure.string/trim @!text)]
                           (when (seq t)
                             (rf/dispatch [:ai/send-message t])
                             (reset! !text "")))))}]
      [ui/button
       {:size     "icon-sm"
        :disabled (or loading? (empty? (clojure.string/trim @!text)) (not provider))
        :on-click (fn []
                    (let [t (clojure.string/trim @!text)]
                      (when (seq t)
                        (rf/dispatch [:ai/send-message t])
                        (reset! !text ""))))}
       [ic/icon-send {:size 16}]]]]))

;; ============================================================
;; Left pane: conversation
;; ============================================================

(defn- conversation-pane []
  (let [messages @(rf/subscribe [:ai/conversation])
        loading? @(rf/subscribe [:ai/loading?])
        provider @(rf/subscribe [:ai/active-provider-data])]
    [:div.flex.flex-col.flex-1.overflow-hidden

     ;; Header
     [:div.flex.items-center.justify-between.px-4.py-3.border-b.border-border
      {:class "bg-card"}
      [:div.flex.items-center.gap-3
       [:div.size-9.rounded-full.flex.items-center.justify-center
        {:class "bg-ai-purple/20 border border-ai-purple/30"}
        [ic/icon-ai {:size 18 :class "text-ai-purple"}]]
       [:div
        [:h3.font-semibold.text-sm "AI Assistant"]
        [:p.text-xs.text-muted-foreground
         (if provider
           (str (provider-label (name (:kind provider))) " / " (:model provider))
           "No provider configured")]]]

      [:div.flex.items-center.gap-1
       [ui/button {:variant "ghost" :size "icon-sm"
                   :title   "Clear conversation"
                   :on-click #(rf/dispatch [:ai/clear-conversation])}
        [ic/icon-delete {:size 16}]]]]

     ;; Messages
     [ui/scroll-area {:class "flex-1 p-4"}
      (if (empty? messages)
        [:div.flex.flex-col.items-center.justify-center.h-full.gap-4.text-center
         {:class "min-h-64"}
         [:div.size-16.rounded-full.flex.items-center.justify-center
          {:class "bg-ai-purple/10 border border-ai-purple/20"}
          [ic/icon-ai {:size 28 :class "text-ai-purple/60"}]]
         [:div
          [:h3.font-medium.text-foreground "Start a conversation"]
          [:p.text-sm.text-muted-foreground.mt-1
           "Ask the AI to draft messages, summarise chats,"
           [:br]
           "suggest replies, or help with business automation."]]]

        [:div
         (for [[idx msg] (map-indexed vector messages)]
           ^{:key idx}
           [ai-bubble msg])
         (when loading?
           [thinking-indicator])])]

     [ai-compose]]))

;; ============================================================
;; Provider card in settings pane
;; ============================================================

(defn- provider-card [provider]
  (let [active @(rf/subscribe [:ai/active-provider])
        active? (= (:id provider) active)
        info (get provider-info (name (:kind provider)))]
    [:button.w-full.text-left.p-3.rounded-lg.border.transition-colors
     {:class    (if active?
                  "border-primary bg-primary/10"
                  "border-border bg-card hover:bg-accent/50")
      :on-click #(rf/dispatch [:ai/set-active-provider (:id provider)])}
     [:div.flex.items-center.justify-between
      [:div.flex.items-center.gap-2
       [:div.size-7.rounded-md.flex.items-center.justify-center
        {:class (if active? "bg-primary/20" "bg-muted")}
        [ic/icon-ai {:size 14 :class (:color info)}]]
       [:div
        [:p.text-sm.font-medium (:name provider)]
        [:p.text-xs.text-muted-foreground (:model provider)]]]
      (when active?
        [:div.size-2.rounded-full {:class "bg-primary"}])]]))

;; ============================================================
;; Right pane: provider config
;; ============================================================

(defn- config-pane []
  (let [providers @(rf/subscribe [:ai/providers])
        !show-add (r/atom false)
        !new-provider (r/atom {:kind "qwen" :name "" :model "" :api-key ""})]
    [:div.flex.flex-col.h-full.border-l.border-border
     {:style {:width "340px" :min-width "280px"}}

     ;; Header
     [:div.px-4.py-3.border-b.border-border
      [:div.flex.items-center.justify-between
       [:h3.font-semibold.text-sm "AI Providers"]
       [ui/button {:size    "icon-sm"
                   :variant "ghost"
                   :on-click #(swap! !show-add not)}
        (if @!show-add
          [ic/icon-close {:size 16}]
          [ic/icon-plus {:size 16}])]]]

     [ui/scroll-area {:class "flex-1"}
      [:div.p-4.flex.flex-col.gap-3
       (if (empty? providers)
         [:div.text-center.py-8.text-muted-foreground
          [ic/icon-ai {:size 32 :class "mx-auto mb-2 opacity-30"}]
          [:p.text-sm "No providers configured"]
          [:p.text-xs.mt-1 "Click + to add your first AI provider"]]
         (for [p providers]
           ^{:key (:id p)}
           [provider-card p]))
       (when @!show-add
         [:div.mt-2.p-4.rounded-xl.border.border-border.bg-card.flex.flex-col.gap-3
          [:h4.font-medium.text-sm "Add Provider"]
          [ui/button
           {:class    "w-full"
            :on-click (fn []
                        (rf/dispatch [:ai/save-provider @!new-provider])
                        (reset! !new-provider {:kind "qwen" :name "" :model "" :api-key ""})
                        (reset! !show-add false))}
           "Save Provider"]])]]]))

;; ============================================================
;; AI view panel
;; ============================================================

(defn panel []
  [:div.flex.flex-1.overflow-hidden
   [conversation-pane]
   [config-pane]])
