(ns whatsauto.views.contacts
  "Contacts view — searchable contact list with detail panel."
  (:require
   [re-frame.core :as rf]
   [whatsauto.interop.shadcn :as ui]
   [whatsauto.interop.icons :as ic]))

(defn- initials [name]
  (when name
    (->> (clojure.string/split name #" ")
         (take 2) (map first) (apply str)
         clojure.string/upper-case)))

(defn- contact-item [{:keys [id jid name phone_number businessName tags]}]
  [:div.flex.items-center.gap-3.px-4.py-3.rounded-lg
   .hover:bg-accent.cursor-pointer.transition-colors

   [ui/avatar
    [ui/avatar-fallback {:class "bg-primary/20 text-primary"}
     (initials name)]]

   [:div.flex-1.min-w-0
    [:p.font-medium.text-sm.text-foreground.truncate name]
    [:p.text-xs.text-muted-foreground.truncate
     (or businessName phone_number jid)]]

   (when (seq tags)
     [:div.flex.gap-1.flex-wrap
      (for [tag tags]
        ^{:key tag}
        [ui/badge {:variant "secondary" :class "text-xs"} tag])])])

(defn panel []
  (let [contacts @(rf/subscribe [:contacts/filtered])
        loading? @(rf/subscribe [:contacts/loading?])
        query    @(rf/subscribe [:contacts/search])]
    [:div.flex.flex-col.flex-1.p-6.overflow-hidden

     ;; Header
     [:div.flex.items-center.justify-between.mb-6
      [:div
       [:h2.text-xl.font-semibold "Contacts"]
       [:p.text-sm.text-muted-foreground
        (str (count contacts) " contacts")]]

      [ui/button {:size "sm"}
       [ic/icon-plus {:size 14}]
       "Add Contact"]]

     ;; Search
     [:div.relative.mb-4
      [:div.absolute.inset-y-0.left-3.flex.items-center.pointer-events-none
       [ic/icon-search {:size 14 :class "text-muted-foreground"}]]
      [:input.w-full.bg-input.rounded-xl.pl-9.pr-4.py-2.text-sm
       .border.border-border.outline-none.text-foreground
       {:class       "focus:border-ring"
        :placeholder "Search contacts..."
        :value       query
        :on-change   #(rf/dispatch [:contacts/set-search (-> % .-target .-value)])}]]

     ;; Contact list
     [ui/scroll-area {:class "flex-1"}
      (if loading?
        [:div.flex.justify-center.py-16
         [ui/spinner {:class "text-primary"}]]

        (if (empty? contacts)
          [:div.flex.flex-col.items-center.justify-center.py-16.text-muted-foreground
           [ic/icon-contacts {:size 40 :class "mb-3 opacity-30"}]
           [:p "No contacts found"]]

          [:div.flex.flex-col.gap-1
           (for [c contacts]
             ^{:key (:id c)}
             [contact-item c])]))]]))
