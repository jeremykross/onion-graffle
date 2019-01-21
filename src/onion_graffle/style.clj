(ns onion-graffle.style
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
   [:#app :.graffle-main {:width "100%"
                          :height "100%"}]])

(def Main
  [:.graffle-main {:display "flex"
                   :flex-direction "column"
                   :position "relative"
                   :height "100%"
                   :width "100%"}
   [(garden.selectors/> "" :.content) {:display "flex"
               :position "relative"
               :flex 1
               :height "100%"}
    [:.nodes {:position "relative"
              :height "100%"
              :flex 1}]]
   [:svg {:pointer-events "none"
          :height "100%"
          :width "100%"
          :z-index -1}]])

(def NewResourceModal
  [:.new-resource-modal {:position "fixed"
                         :background "white"
                         :border-radius "4px"
                         :border (str "1px solid " dark) 
                         :box-shadow shadow
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
    

(def NodeContent
  [:.node-content
   [:h4 {:margin-bottom "8px"}]
   [:p {:font-size "16px"
        :opacity 0.4}]])

(def InformationPanel
  [:.information-panel {:background "white"
                        :border-left (str "1px solid " light)
                        :transition "width 500ms ease"
                        :height "100%"
                        :width "0px"
                        :z-index "5"}
   [:&.open {:width "300px"}]
   [:.content {:padding "32px"}]])

(def TopBar
  [:.top-bar {:align-items "center"
              :border-bottom (str "1px solid " light)
              :display "flex"
              :background "white"
              :min-height "48px"
              :font-size "15px"
              :font-weight "bold"
              :height "88px"
              :posiiton "relative"
              :z-index "5"}
   [:.logo {:font-size "24px"
            :letter-spacing "3px"
            :margin "32px"}]
   [:.title {:position "absolute"
             :flex 1
             :width "100%"
             :text-align "center"}]
    [:.menu {:width "83px"}]])

(def styles [components/ActionButton
             components/BottomBanner
             components/Button
             components/TextInput
             Main
             NewResourceModal
             NodeContent
             Node
             InformationPanel
             TopBar])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css styles)))

