(ns konstellate.graffle.style
  (:require
    garden.selectors
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [onion-components.style :as components]))

(def shadow components/shadow)
(def neutral "#f4f6fc")
(def dark "#081018")
(def light "#d9e1ff")
(def highlight "#00a2ff")

(def Reset
  [[:html :body {:width "100%"
                 :height "100%"}]
   [:button {:background "none"}]
   [:body {:background neutral
           :box-sizing "border-box"
           :color dark
           :font-family "'Rubik', sans-serif"
           :overflow "hidden"
           :margin 0
           :padding 0}]
   [:body [:* {:box-sizing "border-box"
               :font-family "'Rubik', sans-serif"
               :margin 0
               :padding 0
               :-ms-overflow-style "-ms-autohiding-scrollbar"}]]
   [:#app {:width "100%"
           :height "100%"}]])

(def Main
  [:.graffle-main {:display "flex"
                   :flex-direction "column"
                   :position "relative"
                   :height "100%"
                   :width "100%"}
    [:.nodes {:position "relative"
              :height "100%"
              :flex 1}]
    [:svg {:height "100%"
           :width "100%"}]])

(def Node
  [:.node
   {
    :border "1px solid transparent"
    :border-radius "50%"
    :display "flex"
    :justify-content "center"
    :align-items "center"
    :position "absolute"
    :left "-1px"
    :top "-1px"
    :transform-origin "50% 50%"
    :transition "padding 250ms ease, border 250ms ease"
    :user-select "none"}
   [:&:hover {:border (str "1px solid " light)
              :padding "32px"}]
   [:.outline
    {:background "white"
     :border-radius "50%"
     :box-shadow shadow
     :border "1px solid transparent"
     :height "80px"
     :width "80px"}
    [:&.selected {:border (str "1px solid " highlight)}]
    ]])
    

(def styles [Reset
             Main
             Node])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css styles)))

