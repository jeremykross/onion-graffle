(ns onion-graffle.core
  (:require
    recurrent.drivers.dom
    clojure.set
    [onion-components.core :as onion-components]
    [onion-graffle.components :as components]
    [onion-graffle.util :as util]
    [recurrent.core :as recurrent :include-macros true]
    [recurrent.state :as state]
    [ulmus.mouse :as mouse]
    [ulmus.signal :as ulmus]))

(defn Main
  [props sources]
  
  (def state-$ (:recurrent/state-$ sources))
  
  (let [action-button (onion-components/ActionButton {} sources)
        new-resource-modal (components/NewResourceModal {} sources)
        modal-showing?-$ 
        (ulmus/merge
          (ulmus/map (constantly false) (:creation-requests-$ new-resource-modal))
          (ulmus/map (constantly true) (:click-$ action-button)))
        nodes-$ ((util/transduce-state 
                   :enter (fn [nodes [k v]]
                            (assoc nodes 
                                   k (components/Node props sources))))
                 (ulmus/map :resources state-$))
        connect-$ 
        (ulmus/merge
          (ulmus/pickmerge :connect-$ (ulmus/map vals nodes-$))
          (ulmus/map (constantly {:type :connect-off}) mouse/mouseup-$))
        line-$ (ulmus/map
                 (fn [[connect mouse-pos]]
                   (if (= (:type connect)
                          :connect-from)
                     [@(:position-$ connect) mouse-pos]))
                 (ulmus/zip
                   connect-$
                   mouse/position-$))
        connect-pairs-$ (ulmus/filter #(= (count %) 2)
                          (ulmus/reduce
                            (fn [pair connection]
                              (if (= (:type connection) :connect-off)
                                []
                                (conj pair connection)))
                            []
                            connect-$))]

    (ulmus/subscribe! connect-pairs-$ println)

    {:recurrent/state-$ (ulmus/map (fn [new-resource]
                                     (fn [state]
                                       (assoc-in state [:resources (keyword (gensym))] new-resource)))
                                   (:creation-requests-$ new-resource-modal))
     :recurrent/dom-$ (ulmus/map
                        (fn [[modal-showing? line nodes new-resource-modal-dom action-button-dom]]
                          `[:div {:id "main"}
                            ~action-button-dom
                            [:svg/svg
                              ~(if-let [[[x0 y0] [x1 y1]] line]
                                 [:svg/line {:x1 x0 :y1 y0
                                             :x2 x1 :y2 y1
                                             :stroke "lightgrey"}])]
                            ~@nodes
                            ~(if modal-showing?
                               new-resource-modal-dom)])
                        (ulmus/zip
                          modal-showing?-$
                          line-$
                          (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals nodes-$))
                          (:recurrent/dom-$ new-resource-modal)
                          (:recurrent/dom-$ action-button)))}))

(defn main!
  []
  (recurrent/start!
    (state/with-state Main)
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
    
