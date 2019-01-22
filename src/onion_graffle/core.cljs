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

(defn Graffle 
  [props sources]
  
  (def state-$ (:recurrent/state-$ sources))
  
  (let [top-bar (components/TopBar {} sources)
        action-button (onion-components/ActionButton {} sources)
        new-resource-modal (components/NewResourceModal {} sources)
        modal-showing?-$ 
        (ulmus/merge
          (ulmus/map (constantly false) (:creation-requests-$ new-resource-modal))
          (ulmus/map (constantly true) (:click-$ action-button)))

        mouse-pos-$
        (ulmus/map
          (fn [e]
            (let [bounds (.getBoundingClientRect (.-currentTarget e))]
              [(- (.-clientX e) (.-left bounds))
               (- (.-clientY e) (.-top bounds))]))
          ((:recurrent/dom-$ sources) ".nodes" "mousemove"))

        selected-node-id-$
        (ulmus/merge
          (ulmus/map (fn [e]
                       (.stopPropagation e)
                       (.-id (.-currentTarget e)))
                     ((:recurrent/dom-$ sources) ".node" "mousedown"))
          (ulmus/map (constantly false)
                     (ulmus/filter 
                       #(= (.-currentTarget %) (.-target %))
                       ((:recurrent/dom-$ sources) ".nodes" "mousedown"))))

        info-panel (components/InformationPanel
                     {}
                     (assoc sources :open?-$ (ulmus/start-with! false selected-node-id-$)))

        nodes-$
        ((util/transduce-state 
           :enter (fn [nodes [k v]]
                    (assoc nodes 
                           k (components/Node {:id k} (assoc sources
                                                             :selected-node-id-$ selected-node-id-$
                                                             :mouse-pos-$ mouse-pos-$)))))
         (ulmus/map :resources state-$))

        connect-$ 
        (ulmus/merge
          (ulmus/pickmerge :connect-$ (ulmus/map vals nodes-$))
          (ulmus/map (constantly {:type :connect-off}) mouse/mouseup-$))

        line-$
        (ulmus/map
          (fn [[connect mouse-pos]]
            (if (= (:type connect)
                   :connect-from)
              [@(:position-$ connect) mouse-pos]))
          (ulmus/zip
            connect-$
            mouse-pos-$))

        connect-pairs-$
        (ulmus/filter #(and (= (count %) 2)
                            (not= (:position-$ (first %))
                                  (:position-$ (second %))))
                      (ulmus/reduce
                        (fn [pair connection]
                          (if (= (:type connection) :connect-off)
                            []
                            (conj pair connection)))
                        []
                        connect-$))

        relationship-lines-$
        (ulmus/reduce
          (fn [lines [from to]]
            (conj lines (components/RelationshipLine
                          {} (assoc sources
                                    :from-pos-$ (:position-$ from)
                                    :to-pos-$ (:position-$ to)))))
          []
          connect-pairs-$)

        selected-node-pos-$ (ulmus/map
                              (fn [[nodes id]]
                                (when-let [position-$ (get-in nodes [(keyword id) :position-$])]
                                  {:id (keyword id)
                                   :position @position-$}))
                              (ulmus/zip nodes-$ selected-node-id-$))

        selected-resource-$ (ulmus/map
                              (fn [[state id]]
                                (get-in state [:resources (keyword id)]))
                              (ulmus/zip (:recurrent/state-$ sources)
                                         selected-node-id-$))]

    {:edit-$ (ulmus/sample-on selected-node-pos-$ (:edit-$ info-panel))
     :recurrent/state-$
     (ulmus/map (fn [new-resource]
                  (fn [state]
                    (assoc-in state [:resources (keyword (gensym))] new-resource)))
                (:creation-requests-$ new-resource-modal))
     :recurrent/dom-$
     (ulmus/map
       (fn [[modal-showing?
             line
             placed-lines
             nodes
             top-bar-dom
             info-panel-dom
             new-resource-modal-dom
             action-button-dom]]
         (let [draw-line (fn [stroke [[x0 y0] [x1 y1]]]
                           ^{:hipo/key "drawn-line"}
                           [:svg/line {:on-click #(js/console.log "foo")
                                       :x1 x0 :y1 y0
                                       :x2 x1 :y2 y1
                                       :stroke-width 2
                                       :stroke stroke}])]

           ^{:hipo/key "graffle-main"}
           `[:div {:id "graffle-main" :class "graffle-main"}
             ~top-bar-dom
             ^{:hipo/key "content"}
             [:div {:class "content"}
              ^{:hipo/key "nodes"}
              [:div {:class "nodes"}
               ~@nodes
               ^{:hipo/key "svg"}
               [:svg/svg
                ~@placed-lines
                ~(if line (draw-line "#00a2ff" line))]
               ~action-button-dom]
              ~info-panel-dom]
             ~(if modal-showing?
                new-resource-modal-dom)]))
       (ulmus/zip
         modal-showing?-$
         line-$
         (ulmus/pickzip :recurrent/dom-$ relationship-lines-$)
         (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals nodes-$))
         (:recurrent/dom-$ top-bar)
         (:recurrent/dom-$ info-panel)
         (:recurrent/dom-$ new-resource-modal)
         (:recurrent/dom-$ action-button)))}))

(defn main!
  []
  (recurrent/start!
    (state/with-state Graffle)
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
    
