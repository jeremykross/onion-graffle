(ns konstellate.graffle.style
  (:require
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [garden.selectors :as sel]
    [konstellate.components.style :as components]))

(def shadow components/shadow)
(def neutral "#f4f6fc")
(def dark "#081018")
(def light "#d9e1ff")
(def highlight "#00a2ff")

(def text "#b0bac9")

(defkeyframes FadeInAnim
  [:from {:opacity 0
          :transform "scale(0.95, 0.95)"}]
  [:to {:opacity 1
        :transform "scale(1,1)"}])

(def Reset
  [[:html :body {:width "100%"
                 :height "100%"}]
   [:button {:background "none"}]
   [:body {:background "black"
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
           :width "100%"}
     [:.relationship-line {:transition "stroke 1000ms ease, stroke-width 1000ms ease"}]]])

(def Node
  [[:.node.selected {}
    [:.outline {:background highlight
                :opacity 1}]
    [:.name {:color highlight}]]
   [:.node.deselected {}
    [:.outline {:background "#a2a18a"}]]
   [:.node:hover
    [:.outline {:padding "16px"}]]
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
     :transition "padding 800ms ease, border 250ms ease"
     :user-select "none"}
    [:&:hover {:padding "24px"}]
    [:.outline
     {:background "#fff6c2"
      :border-radius "50%"
      :border "1px solid transparent"
      :height "16px"
      :transition "background 1000ms ease, opacity 1000ms ease, padding 400ms ease"
      :width "16px"}]

    [:.name 
     {:line-height "100%"
      :color "white"
      :font-weight "bold"
      :position "absolute"
      :pointer-events "none"
      :bottom "-40px"
      :left "50%"
      :text-align "center"
      :white-space "nowrap"
      :font-size "12px"
      :transition "color 1000ms ease"
      :transform "translateX(-50%)"}
     [:span {:color "rgba(255,255,255,0.5)"
             :display "block"
             :font-size "10px"
             :text-align "center"
             :margin-bottom "4px"
             :text-transform "uppercase"}]]]])

(def ConnectionModalContent
  [:.connection-modal-content
   {:box-shadow shadow}
   [:.padded {:padding "32px"}]
   [:h3
    {:font-size "20px"
     :letter-spacing "1px"}]

   [:.picker {:display "grid"
              :grid-template-columns "auto auto"
              :grid-gap "40px"
              :margin "52px 0 32px"}]
   [:select {:width "420px"}]
   [:.banner {:align-items "center"
              :background "#081018"
              :height "82px"
              :display "flex"
              :justify-content "flex-end"}
    [:.button {:margin-right "32px"
               :width "136px"}]]])

(def styles [Reset
             components/Button
             components/Modal
             components/SelectionModal
             components/Select
             components/ModalFadeIn
             ConnectionModalContent
             FadeInAnim
             Main
             Node])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css styles)))

