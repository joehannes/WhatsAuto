(ns whatsauto.interop.shadcn
  "ClojureScript wrappers for ShadCN UI components.

   Usage:
     [:> shadcn/button {:on-click f :variant \"outline\"} \"Click me\"]

   Or the Reagent-friendly versions:
     [button {:on-click f :variant \"outline\"} \"Click me\"]

   All props are converted from Clojure maps to JS objects.
   Children are passed through as-is."
  (:require
   [reagent.core :as r]
   ["WhatsAutoShadcn" :refer
    [Button
     Card CardHeader CardTitle CardDescription CardContent CardFooter
     Badge
     Avatar AvatarImage AvatarFallback
     Input Textarea Separator ScrollArea
     Tooltip TooltipTrigger TooltipContent TooltipProvider
     Dialog DialogContent DialogHeader DialogTitle DialogDescription DialogFooter
     DropdownMenu DropdownMenuTrigger DropdownMenuContent DropdownMenuItem DropdownMenuSeparator
     Select SelectTrigger SelectValue SelectContent SelectItem
     Switch
     Tabs TabsList TabsTrigger TabsContent
     Skeleton Spinner
     SidebarProvider Sidebar SidebarContent SidebarHeader SidebarFooter
     SidebarMenu SidebarMenuItem SidebarMenuButton SidebarGroup SidebarGroupLabel
     SidebarGroupContent SidebarTrigger SidebarInset SidebarMenuBadge SidebarSeparator
     Toaster toast]]))

;; ============================================================
;; Component adapter helpers
;; ============================================================

(defn- adapt
  "Convert a Clojure component to a Reagent-compatible component
   by wrapping with r/adapt-react-class."
  [component]
  (r/adapt-react-class component))

;; ============================================================
;; Adapted ShadCN components
;; ============================================================

(def button          (adapt Button))
(def card            (adapt Card))
(def card-header     (adapt CardHeader))
(def card-title      (adapt CardTitle))
(def card-description (adapt CardDescription))
(def card-content    (adapt CardContent))
(def card-footer     (adapt CardFooter))
(def badge           (adapt Badge))
(def avatar          (adapt Avatar))
(def avatar-image    (adapt AvatarImage))
(def avatar-fallback (adapt AvatarFallback))
(def input           (adapt Input))
(def textarea        (adapt Textarea))
(def separator       (adapt Separator))
(def scroll-area     (adapt ScrollArea))
(def tooltip-provider (adapt TooltipProvider))
(def tooltip         (adapt Tooltip))
(def tooltip-trigger (adapt TooltipTrigger))
(def tooltip-content (adapt TooltipContent))
(def dialog          (adapt Dialog))
(def dialog-content  (adapt DialogContent))
(def dialog-header   (adapt DialogHeader))
(def dialog-title    (adapt DialogTitle))
(def dialog-description (adapt DialogDescription))
(def dialog-footer   (adapt DialogFooter))
(def dropdown-menu   (adapt DropdownMenu))
(def dropdown-menu-trigger (adapt DropdownMenuTrigger))
(def dropdown-menu-content (adapt DropdownMenuContent))
(def dropdown-menu-item (adapt DropdownMenuItem))
(def dropdown-menu-separator (adapt DropdownMenuSeparator))
(def select          (adapt Select))
(def select-trigger  (adapt SelectTrigger))
(def select-value    (adapt SelectValue))
(def select-content  (adapt SelectContent))
(def select-item     (adapt SelectItem))
(def switch          (adapt Switch))
(def tabs            (adapt Tabs))
(def tabs-list       (adapt TabsList))
(def tabs-trigger    (adapt TabsTrigger))
(def tabs-content    (adapt TabsContent))
(def skeleton        (adapt Skeleton))
(def spinner         (adapt Spinner))
(def toaster         (adapt Toaster))

;; Sidebar components
(def sidebar-provider      (adapt SidebarProvider))
(def sidebar               (adapt Sidebar))
(def sidebar-content       (adapt SidebarContent))
(def sidebar-header        (adapt SidebarHeader))
(def sidebar-footer        (adapt SidebarFooter))
(def sidebar-menu          (adapt SidebarMenu))
(def sidebar-menu-item     (adapt SidebarMenuItem))
(def sidebar-menu-button   (adapt SidebarMenuButton))
(def sidebar-group         (adapt SidebarGroup))
(def sidebar-group-label   (adapt SidebarGroupLabel))
(def sidebar-group-content (adapt SidebarGroupContent))
(def sidebar-trigger       (adapt SidebarTrigger))
(def sidebar-inset         (adapt SidebarInset))
(def sidebar-menu-badge    (adapt SidebarMenuBadge))
(def sidebar-separator     (adapt SidebarSeparator))

;; ============================================================
;; Toast helper
;; ============================================================

(defn show-toast
  ([msg] (toast msg))
  ([msg opts] (toast msg (clj->js opts))))
