(ns konstellate.graffle.style
  (:require
    garden.selectors
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [konstellate.components.style :as components]))

(def shadow components/shadow)
(def neutral "#f4f6fc")
(def dark "#081018")
(def light "#d9e1ff")
(def highlight "#00a2ff")

(def text "#b0bac9")

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
                   :overflow "hidden"
                   :height "100%"
                   :width "100%"}
    [:.nodes {:position "relative"
              :height "100%"
              :flex 1}]
    [:svg {:height "100%"
           :width "100%"}]])

(def Node
  [:.node
   {:background "rgba(255, 255, 255, 0.2)"
    :border "1px solid transparent"
    :border-radius "50%"
    :display "flex"
    :justify-content "center"
    :align-items "center"
    :padding "8px"
    :position "absolute"
    :left "-1px"
    :top "-1px"
    :transform-origin "50% 50%"
    :transition "padding 250ms ease, border 250ms ease"
    :user-select "none"}
   [:&:hover {:padding "32px"}]
   [:.outline
    {:background "#fff6c2"
     :border-radius "50%"
     :border "1px solid transparent"
     :height "16px"
     :width "16px"}]

   [:.name {:line-height "100%"
            :color text
            :font-weight "bold"
            :position "absolute"
            :pointer-events "none"
            :bottom "-32px"
            :left "50%"
            :text-align "center"
            :white-space "nowrap"
            :font-size "14px"
            :transform "translateX(-50%)"}]])
    

(def styles [Reset
             Main
             Node])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css styles)))

