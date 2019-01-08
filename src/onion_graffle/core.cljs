(ns onion-graffle.core
  (:require
    recurrent.drivers.dom
    [onion-components.core :as onion-components]
    [onion-graffle.components :as components]
    [recurrent.core :as recurrent :include-macros true]
    [recurrent.state :as state]
    [ulmus.signal :as ulmus]))

(defn Main
  [props sources]
  
  (def state-$ (:recurrent/state-$ sources))
  
  (let [action-button (onion-components/ActionButton {} sources)
        new-resource-modal (components/NewResourceModal {} sources)
        modal-showing?-$ (ulmus/reduce not false (:click-$ action-button))]

    (ulmus/subscribe! (:creation-requests-$ new-resource-modal) println)

    {:recurrent/state-$ (ulmus/map (fn [new-resource]
                                     (fn [state]
                                       (assoc-in state [:resources (keyword (gensym))] new-resource)))
                                   (:creation-requests-$ new-resource-modal))
     :recurrent/dom-$ (ulmus/map
                        (fn [[modal-showing? new-resource-modal-dom action-button-dom]]
                          [:div {:id "main"}
                           action-button-dom
                           (if modal-showing?
                             new-resource-modal-dom)])
                        (ulmus/zip
                          modal-showing?-$
                          (:recurrent/dom-$ new-resource-modal)
                          (:recurrent/dom-$ action-button)))}))


(defn main!
  []
  (recurrent/start!
    (state/with-state Main)
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
    
