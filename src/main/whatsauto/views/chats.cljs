(ns whatsauto.views.chats
  "Chats view: two-pane layout with chat list and active conversation."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.interop.icons :as ic]))

;; ============================================================
;; Helpers
;; ============================================================

(defn- format-time [ts]
  (when ts
    (let [d (js/Date. (* ts 1000))]
      (.toLocaleTimeString d js/undefined #js {:hour "2-digit" :minute "2-digit"}))))

(defn- initials [name]
  (when name
    (->> (clojure.string/split name #" ")
         (take 2)
         (map first)
         (apply str)
         clojure.string/upper-case)))

;; ============================================================
;; Chat list item
;; ============================================================

(defn- chat-list-item [{:keys [id name lastMessage unreadCount timestamp isGroup]}]
  (let [active-id @(rf/subscribe [:chats/active-id])
        active?   (= id active-id)
        typing?   (contains? @(rf/subscribe [:ui/typing-chats]) id)]
    [:button.w-full.flex.items-start.gap-3.px-4.py-3.text-left.transition-colors
     {:class    (if active? "bg-accent" "hover:bg-accent/50")
      :on-click #(rf/dispatch [:chats/select id])}

     ;; Avatar
     [ui/avatar {:size "default"}
      [ui/avatar-fallback
       {:class (if isGroup "bg-primary/20 text-primary" "bg-secondary")}
       (initials name)]]

     ;; Content
     [:div.flex-1.min-w-0
      [:div.flex.items-baseline.justify-between.gap-2
       [:span.font-medium.text-sm.text-foreground.truncate name]
       [:span.text-xs.text-muted-foreground.shrink-0 (format-time timestamp)]]

      [:div.flex.items-center.justify-between.mt-0.5
       (if typing?
         [:span.text-xs.text-primary.italic "typing..."]
         [:span.text-xs.text-muted-foreground.truncate
          (or (:body lastMessage) (get-in lastMessage [:body]) "")])

       (when (pos? unreadCount)
         [:span.inline-flex.items-center.justify-center.size-5.rounded-full.text-xs.font-medium.shrink-0
          {:class "bg-primary text-primary-foreground"}
          (if (> unreadCount 99) "99+" unreadCount)])]]]))

;; ============================================================
;; Chat list panel
;; ============================================================

(defn- chat-list-panel []
  (let [chats   @(rf/subscribe [:chats/filtered])
        loading @(rf/subscribe [:chats/loading?])
        query   @(rf/subscribe [:chats/search-query])]
    [:div.flex.flex-col.h-full.border-r.border-border
     {:style {:width "320px" :min-width "280px" :max-width "380px"}}

     ;; Header
     [:div.px-4.py-3.border-b.border-border
      [:div.flex.items-center.justify-between.mb-3
       [:h2.font-semibold.text-foreground "Chats"]
       [ui/button {:variant "ghost" :size "icon-sm"
                   :on-click #(rf/dispatch [:chats/load])}
        [ic/icon-refresh {:size 16}]]]

      ;; Search input
      [:div.relative
       [:div.absolute.inset-y-0.left-2.5.flex.items-center.pointer-events-none
        [ic/icon-search {:size 14 :class "text-muted-foreground"}]]
       [:input.w-full.bg-input.rounded-lg.pl-8.pr-3.py-1.5.text-sm.text-foreground.placeholder-muted-foreground.border.border-border.outline-none
        {:class       "focus:border-ring focus:ring-1 focus:ring-ring/50"
         :placeholder "Search chats..."
         :value       query
         :on-change   #(rf/dispatch [:chats/set-search (-> % .-target .-value)])}]]]

     ;; Chat list
     [ui/scroll-area {:class "flex-1"}
      (if loading
        [:div.flex.flex-col.gap-2.p-4
         (for [i (range 5)]
           ^{:key i}
           [:div.flex.gap-3.items-center
            [ui/skeleton {:class "size-10 rounded-full"}]
            [:div.flex-1
             [ui/skeleton {:class "h-4 w-3/4 mb-2"}]
             [ui/skeleton {:class "h-3 w-1/2"}]]])]

        (if (empty? chats)
          [:div.flex.flex-col.items-center.justify-center.h-48.text-muted-foreground
           [ic/icon-chats {:size 32 :class "mb-2 opacity-30"}]
           [:p.text-sm "No chats yet"]]

          [:div.py-1
           (for [chat chats]
             ^{:key (:id chat)}
             [chat-list-item chat])]))]]))

;; ============================================================
;; Message bubble
;; ============================================================

(defn- message-bubble [{:keys [body fromMe timestamp type aiProvider]}]
  [:div.flex.mb-2
   {:class (if fromMe "justify-end" "justify-start")}
   [:div.max-w-xs.lg:max-w-md.px-3.py-2.rounded-xl.text-sm
    {:class (if fromMe
              "bubble-outgoing text-foreground"
              "bubble-incoming text-foreground border border-border")}

    ;; AI indicator
    (when (and fromMe aiProvider)
      [:div.flex.items-center.gap-1.mb-1
       [ic/icon-ai {:size 10 :class "text-ai-purple"}]
       [:span.text-xs {:class "text-ai-purple/80"} (str "via " aiProvider)]])

    [:p.leading-relaxed body]

    [:div.flex.items-center.justify-end.gap-1.mt-1
     [:span.text-xs.opacity-60 (format-time timestamp)]
     (when fromMe
       [ic/icon-check-check {:size 12 :class "opacity-60"}])]]])

;; ============================================================
;; Conversation mode badge
;; ============================================================

(defn- mode-badge [mode]
  (case mode
    "ai"
    [ui/badge {:variant "default" :class "bg-ai-purple text-white text-xs gap-1"}
     [ic/icon-ai {:size 10}] "AI"]
    "hybrid"
    [ui/badge {:variant "outline" :class "text-xs gap-1 border-ai-purple text-ai-purple"}
     [ic/icon-ai {:size 10}] "Hybrid"]
    [ui/badge {:variant "secondary" :class "text-xs gap-1"}
     [ic/icon-user {:size 10}] "Human"]))

;; ============================================================
;; Compose bar
;; ============================================================

(defn- compose-bar [chat-id]
  (let [text @(rf/subscribe [:ui/compose-text])]
    [:div.border-t.border-border.px-4.py-3
     [:div.flex.items-end.gap-2
      ;; Attach
      [ui/button {:variant "ghost" :size "icon-sm"
                  :class "text-muted-foreground shrink-0 self-end mb-1"}
       [ic/icon-attach {:size 18}]]

      ;; Textarea (auto-grows)
      [:div.flex-1
       [:textarea.w-full.bg-input.rounded-xl.px-3.py-2.text-sm.text-foreground.placeholder-muted-foreground.border.border-border.outline-none.resize-none.max-h-32.min-h-9
        {:class       "focus:border-ring focus:ring-1 focus:ring-ring/50"
         :placeholder "Type a message..."
         :value       text
         :rows        1
         :on-change   #(rf/dispatch [:ui/set-compose-text (-> % .-target .-value)])
         :on-key-down (fn [e]
                        (when (and (= 13 (.-keyCode e))
                                   (not (.-shiftKey e)))
                          (.preventDefault e)
                          (when (seq (clojure.string/trim text))
                            (rf/dispatch [:messages/send-text chat-id text]))))}]]

      ;; Send
      [ui/button {:variant  "default"
                  :size     "icon-sm"
                  :disabled (empty? (clojure.string/trim text))
                  :on-click #(when (seq (clojure.string/trim text))
                               (rf/dispatch [:messages/send-text chat-id text]))
                  :class    "shrink-0 self-end"}
       [ic/icon-send {:size 16}]]]]))

;; ============================================================
;; Conversation header
;; ============================================================

(defn- conversation-header [chat]
  [:div.flex.items-center.gap-3.px-4.py-3.border-b.border-border
   {:class "bg-card"}

   [ui/avatar {:size "default"}
    [ui/avatar-fallback {:class "bg-primary/20 text-primary"}
     (initials (:name chat))]]

   [:div.flex-1.min-w-0
    [:div.flex.items-center.gap-2
     [:h3.font-semibold.text-sm.text-foreground.truncate (:name chat)]
     [mode-badge (:conversationMode chat)]]
    [:p.text-xs.text-muted-foreground
     (if (:isGroup chat) "Group" (str "+ " (:jid chat)))]]

   ;; Action buttons
   [:div.flex.items-center.gap-1
    ;; Mode toggle
    [ui/dropdown-menu
     [ui/dropdown-menu-trigger
      [ui/button {:variant "ghost" :size "icon-sm"}
       [ic/icon-ai {:size 16}]]]
     [ui/dropdown-menu-content {:align "end"}
      [ui/dropdown-menu-item
       {:on-click #(rf/dispatch [:ai/set-conversation-mode (:id chat) "human"])}
       [ic/icon-user {:size 14}] "Human mode"]
      [ui/dropdown-menu-item
       {:on-click #(rf/dispatch [:ai/set-conversation-mode (:id chat) "ai"])}
       [ic/icon-ai {:size 14}] "AI mode"]
      [ui/dropdown-menu-item
       {:on-click #(rf/dispatch [:ai/set-conversation-mode (:id chat) "hybrid"])}
       [ic/icon-ai {:size 14}] "Hybrid mode"]]]

    [ui/button {:variant "ghost" :size "icon-sm"}
     [ic/icon-phone {:size 16}]]
    [ui/button {:variant "ghost" :size "icon-sm"}
     [ic/icon-more {:size 16}]]]])

;; ============================================================
;; Conversation view (right pane)
;; ============================================================

(defn- conversation-view []
  (let [active-chat @(rf/subscribe [:chats/active])
        messages    @(rf/subscribe [:messages/active-chat])
        loading?    @(rf/subscribe [:messages/loading?])]

    (if-not active-chat
      ;; Empty state
      [:div.flex.flex-1.flex-col.items-center.justify-center.text-muted-foreground.gap-4
       [:div.size-24.rounded-full.flex.items-center.justify-center
        {:class "bg-primary/5 border border-primary/20"}
        [ic/icon-chats {:size 40 :class "text-primary/40"}]]
       [:div.text-center
        [:h3.font-medium.text-foreground "Select a conversation"]
        [:p.text-sm.mt-1 "Choose a chat from the list to start messaging"]]]

      ;; Active conversation
      [:div.flex.flex-col.flex-1.overflow-hidden
       [conversation-header active-chat]

       ;; Messages area
       [ui/scroll-area {:class "flex-1 p-4"}
        (if loading?
          [:div.flex.justify-center.py-8
           [ic/icon-loading {:size 24 :class "text-primary animate-spin"}]]

          [:div.flex.flex-col.min-h-full.justify-end
           (if (empty? messages)
             [:div.flex.justify-center.py-8.text-muted-foreground
              [:p.text-sm "No messages yet"]]
             (for [msg (reverse messages)]
               ^{:key (:id msg)}
               [message-bubble msg]))])]

       ;; Compose
       [compose-bar (:id active-chat)]])))

;; ============================================================
;; Main chats panel
;; ============================================================

(defn panel []
  [:div.flex.flex-1.overflow-hidden
   [chat-list-panel]
   [conversation-view]])
