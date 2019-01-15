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
        connect-pairs-$ (ulmus/filter #(and (= (count %) 2)
                                            (not= (:position-$ (first %))
                                                  (:position-$ (second %))))
                          (ulmus/reduce
                            (fn [pair connection]
                              (if (= (:type connection) :connect-off)
                                []
                                (conj pair connection)))
                            []
                            connect-$))
        relationships-$ (ulmus/reduce
                          (fn [placed [from to]]
                            (conj placed
                                  [(:position-$ from)
                                   (:position-$ to)]))
                          []
                          connect-pairs-$)
        placed-lines-$ 
        (ulmus/map
          #(partition 2 %)
          (ulmus/pickmap
            (fn [relationships]
              (apply ulmus/zip (flatten relationships)))
            relationships-$))]

    {:recurrent/state-$ (ulmus/map (fn [new-resource]
                                     (fn [state]
                                       (assoc-in state [:resources (keyword (gensym))] new-resource)))
                                   (:creation-requests-$ new-resource-modal))
     :recurrent/dom-$ (ulmus/map
                        (fn [[modal-showing? line placed-lines nodes new-resource-modal-dom action-button-dom]]
                          (let [draw-curve (fn [[[x0 y0] [x1 y1]]]
                                             [:svg/path {:d (str "M" x0 " " y0 " "
                                                                 "Q" (/ x1 2) " " (/ y1 2) "," x1 " " y1)
                                                         :fill "transparent"
                                                         :stroke "lightgrey"}])
                                draw-line (fn [[[x0 y0] [x1 y1]]]
                                            [:svg/line {:x1 x0 :y1 y0
                                                        :x2 x1 :y2 y1
                                                        :stroke "lightgrey"}])]
                            `[:div {:id "main"}
                              ~@nodes
                              ~action-button-dom
                              ^{:hipo/key "svg"}
                              [:svg/svg
                                ~(map draw-line placed-lines)
                                ~(if line (draw-line line))]
                              ~(if modal-showing?
                                 new-resource-modal-dom)]))
                        (ulmus/zip
                          modal-showing?-$
                          line-$
                          placed-lines-$
                          (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals nodes-$))
                          (:recurrent/dom-$ new-resource-modal)
                          (:recurrent/dom-$ action-button)))}))

(defn main!
  []
  (recurrent/start!
    (state/with-state Main)
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
    
