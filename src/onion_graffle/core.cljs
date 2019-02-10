(ns onion-graffle.core
  (:require
    recurrent.drivers.dom
    [onion-graffle.components :as components]
    [onion-graffle.connections :as connections]
    [ulmus.mouse :as mouse]
    [ulmus.signal :as ulmus]
    [recurrent.core :as recurrent]
    [recurrent.state :as state]))

(def initial-state
  {:foo {:kind "Deployment"
         :spec {:template {:metadata {:labels {:app "foobar"}}}}}
   :bar {:kind "Service"
         :spec {:selector {:app "foobar"}}}})

(defn Main
  [props sources]
  (let [selected-node-id-$ (ulmus/map (fn [e]
                                        (keyword
                                          (.getAttribute
                                            (.-currentTarget e)
                                            "id")))
                                      ((:recurrent/dom-$ sources) ".node" "click"))

        connections-$ (ulmus/map
                        (fn [state]
                          (into #{}
                            (flatten
                              (loop [acc []
                                     resources (map (fn [[k r]] (with-meta r {:key k})) state)]
                                (let [tail (rest resources)]
                                  (if (empty? resources) acc
                                    (recur (conj acc (connections/between (first resources) (first tail)))
                                           tail)))))))
                        (:recurrent/state-$ sources))
        nodes-$ (ulmus/reduce
                  (fn [nodes [added removed]]
                    (let [new-nodes 
                          (into {} (map (fn [[k r]]
                                          [k (components/Node
                                               {:id k}
                                               (assoc sources
                                                      :selected-node-id-$ selected-node-id-$
                                                      :mouse-pos-$ mouse/position-$))])
                                        added))]
                      (-> nodes
                          (merge new-nodes)
                          (dissoc (keys removed)))))
                  {}
                  (ulmus/changed-keys
                    (:recurrent/state-$ sources)))
        lines-$ (ulmus/reduce
                  (fn [lines [added removed]]
                    (let [new-lines
                          (map (fn [c] [c (components/RelationshipLine
                                            {}
                                            {:from-pos-$ (ulmus/signal-of [0 0])
                                             :to-pos-$ (ulmus/signal-of [100 100])})])
                               added)]
                      (merge lines (into {} new-lines))))
                  {}
                  (ulmus/zip
                    (ulmus/set-added connections-$)
                    (ulmus/set-removed connections-$)))]

    (ulmus/subscribe! lines-$ (fn [c] (println "Connections:" c)))

    {:recurrent/dom-$ (ulmus/map
                        (fn [nodes-dom]
                          `[:div {:class "nodes"}
                            ~@nodes-dom])
                        (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals nodes-$)))
     :recurrent/state-$ (ulmus/signal-of (fn [] initial-state))}))

(defn main!
  []
  (recurrent/start!
    (state/with-state Main)
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
  
