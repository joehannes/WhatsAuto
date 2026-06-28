(ns whatsauto.views.automation
  "Automation view — scheduler, rules, workflows."
  (:require [re-frame.core :as rf]
            [whatsauto.interop.shadcn :as ui]
            [whatsauto.interop.icons :as ic]))

(defn- status-badge [status]
  (case status
    "pending"   [ui/badge {:variant "outline" :class "text-yellow-500"} "Pending"]
    "running"   [ui/badge {:variant "outline" :class "text-blue-500 animate-pulse"} "Running"]
    "completed" [ui/badge {:variant "outline" :class "text-green-500"} "Done"]
    "failed"    [ui/badge {:variant "outline" :class "text-destructive"} "Failed"]
    "cancelled" [ui/badge {:variant "outline"} "Cancelled"]
    [ui/badge {:variant "outline"} status]))

(defn- scheduler-panel []
  (let [tasks @(rf/subscribe [:scheduler/tasks])]
    [:div.flex.flex-col.h-full
     [:div.flex.items-center.justify-between.mb-4
      [:div
       [:h3.font-semibold "Scheduled Tasks"]
       [:p.text-xs.text-muted-foreground (str (count tasks) " tasks")]]
      [ui/button {:size "sm"}
       [ic/icon-plus {:size 14}] "New Task"]]
     [ui/scroll-area {:class "flex-1"}
      (if (empty? tasks)
        [:div.flex.flex-col.items-center.justify-center.py-12.text-muted-foreground
         [ic/icon-automation {:size 32 :class "mb-2 opacity-30"}]
         [:p.text-sm "No scheduled tasks"]]
        [:div.flex.flex-col.gap-2
         (for [task tasks]
           ^{:key (:id task)}
           [:div.p-3.rounded-lg.border.border-border.bg-card
            [:div.flex.items-center.justify-between
             [:span.text-sm.font-medium (:taskType task)]
             [status-badge (:status task)]]])])]]))

(defn- rules-panel []
  (let [rules @(rf/subscribe [:rules/list])]
    [:div.flex.flex-col.h-full
     [:h3.font-semibold.mb-4 "Business Rules"]
     [ui/scroll-area {:class "flex-1"}
      [:div.flex.flex-col.gap-2
       (for [rule rules]
         ^{:key (:id rule)}
         [:div.p-3.rounded-lg.border.border-border.bg-card
          [:span.text-sm.font-medium (:name rule)]])]]]))

(defn panel []
  (let [active-tab @(rf/subscribe [:automation/tab])]
    [:div.flex.flex-col.flex-1.p-6.overflow-hidden
     [:div.mb-6
      [:h2.text-2xl.font-bold "Automation"]
      [:p.text-muted-foreground "Schedule tasks, define rules, and create workflows"]]
     [ui/tabs {:value (name active-tab)
               :on-value-change #(rf/dispatch [:automation/set-tab (keyword %)])}
      [ui/tabs-list
       [ui/tabs-trigger {:value "scheduler"} "Scheduler"]
       [ui/tabs-trigger {:value "rules"} "Rules"]
       [ui/tabs-trigger {:value "workflows"} "Workflows"]]
      [ui/tabs-content {:value "scheduler"} [scheduler-panel]]
      [ui/tabs-content {:value "rules"} [rules-panel]]
      [ui/tabs-content {:value "workflows"}
       [:div.text-center.py-12.text-muted-foreground
        [ic/icon-zap {:size 32 :class "mb-2 opacity-30"}]
        [:p.text-sm "Workflows coming soon"]]]]]))
