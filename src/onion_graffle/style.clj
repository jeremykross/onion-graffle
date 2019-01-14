(ns onion-graffle.style
  (:require
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [onion-components.style :as components]))

(def shadow "2px 2px 4px rgba(0,0,0, 0.25)")
(def primary "#FEA7BD")
(def secondary "#09EDC8")

(defkeyframes FadeInAnim
  [:from {:opacity 0
          :transform "scale(0.95, 0.95)"}]
  [:to {:opacity 1
        :transform "scale(1, 1)"}])

(def LeftRight [:.left-right
                {:display "grid"
                 :height "100%"
                 :max-height "100%"
                 :grid-gap "16px"
                 :grid-template-columns "1fr 1fr"}
                [:.text-edit {:background "black"
                              :color "lime"
                              :padding "32px"
                              :border "1px solid lightgrey"
                              :border-radius "4px"}]])

(def Reset
  [[:html :body {:width "100%"
                 :height "100%"}]
   [:button {:background "none"}]
   [:body {:box-sizing "border-box"
           :font-family "ubuntu sans-serif"
           :margin 0
           :padding 0}]
   [:body [:* {:box-sizing "border-box"
               :font-family "sans-serif"
               :margin 0
               :padding 0
               :-ms-overflow-style "-ms-autohiding-scrollbar"}]]
   [:#app :#main {:width "100%"
                  :height "100%"}]])

(def Main
  [:#main {:position "relative"}])

(def NewResourceModal
  [:.new-resource-modal {:position "fixed"
                         :background "white"
                         :border-radius "4px"
                         :border "1px solid lightgrey"
                         :box-shadow shadow
                         :color "black"
                         :left "50%"
                         :overflow "hidden"
                         :min-width "480px"
                         :top "50%"
                         :transform "translate(-50%, -50%)"}
   [:h2 {:font-size "18px"}]
   [:.content {:display "grid"
               :grid-gap "16px"
               :padding "32px"}]])

(def Node
  [:.node
   {:border "1px solid lightgrey"
    :border-radius "50%"
    :display "flex"
    :justify-content "center"
    :align-items "center"
    :position "absolute"
    :transform-origin "50% 50%"
    :transition "padding 250ms ease"
    :user-select "none"}
   [:&:hover {:padding "32px"}]
   [:.outline
    {:background "white"
     :border "1px solid lightgrey"
     :border-radius "50%"
     :height "64px"
     :width "64px"}]])
    

(def NodeContent
  [:.node-content
   [:h4 {:margin-bottom "8px"}]
   [:p {:font-size "16px"
        :opacity 0.4}]])


(defn styles
  []
  (garden/css 
    components/ActionButton
    components/BottomBanner
    components/Button
    components/TextInput
    LeftRight
    Main
    NewResourceModal
    NodeContent
    Node
    Reset))

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (styles)))

