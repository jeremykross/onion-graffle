(ns konstellate.graffle.core
  (:require
    clojure.set
    recurrent.drivers.vdom
    recurrent.core
    [konstellate.graffle.components :as components]
    [konstellate.graffle.connections :as connections]
    [ulmus.mouse :as mouse]
    [ulmus.signal :as ulmus]
    [recurrent.core :as recurrent]
    [recurrent.state :as state]))

(def initial-state
  {:foo {:kind "Deployment"
         :spec {:template {:metadata {:labels {:app "foobar"}}}}}
   :bar {:kind "Service"
         :spec {:selector {:app "foobar"}}}})

(recurrent.core/defcomponent Graffle
  [_ sources]
  (let [selected-node-id-$ (ulmus/map (fn [e]
                                        (.stopPropagation e)
                                        (keyword
                                          (.getAttribute
                                            (.-currentTarget e)
                                            "id")))
                                      (ulmus/merge
                                        ((:recurrent/dom-$ sources) ".node" "mousedown")
                                        ((:recurrent/dom-$ sources) ".node" "click")))

        selected-nodes-$ 
        (ulmus/merge
          (ulmus/map (constantly [])
                     ((:recurrent/dom-$ sources) :root "click"))
          (ulmus/map vector selected-node-id-$))

        selected-resources-$ (ulmus/map
                               (fn [[selected-nodes state]]
                                 (into {}
                                       (map (fn [node-id]
                                              [node-id (get state node-id)])
                                            selected-nodes)))
                               (ulmus/zip selected-nodes-$
                                          (:recurrent/state-$ sources)))

        mouse-pos-$ (ulmus/map
                      (fn [e]
                        (let [bounds (.getBoundingClientRect (.-currentTarget e))]
                          [(- (.-clientX e) (.-left bounds))
                           (- (.-clientY e) (.-top bounds))]))
                      ((:recurrent/dom-$ sources) :root "mousemove"))

        nodes-$ (ulmus/reduce
                  (fn [nodes [added removed]]
                    (let [new-nodes 
                          (into {} (map (fn [[k r]]
                                          [k (components/Node
                                               {:id k}
                                               (assoc sources
                                                      :content-$ (ulmus/map
                                                                   #(get-in % [k :metadata :name])
                                                                   (:recurrent/state-$ sources))
                                                      :selected-node-id-$ selected-node-id-$
                                                      :selected-nodes-$ selected-nodes-$
                                                      :mouse-pos-$ mouse-pos-$))])
                                        added))]
                      (-> nodes
                          (merge new-nodes)
                          (dissoc (keys removed)))))
                  {}
                  (ulmus/changed-keys
                    (:recurrent/state-$ sources)))


        connections-$ (ulmus/distinct
                        (ulmus/map
                          (fn [state]
                            (into #{}
                                  (flatten
                                    (loop [acc []
                                           resources (map (fn [[k r]] (with-meta r {:key k})) state)]
                                      (let [tail (rest resources)]
                                        (if (empty? resources) acc
                                          (recur (conj acc (connections/between (first resources) (first tail)))
                                                 tail)))))))
                          (:recurrent/state-$ sources)))

        ; LOOK HERE, running too many times.
        ; Producing too many lines.
        lines-$ (ulmus/reduce
                  (fn [lines [nodes added removed]]
                    (println "Added:" added)
                    (let [new-lines
                          (map (fn [c] 
                                 (let [id (gensym)
                                       from ((:from c) nodes)
                                       to ((:to c) nodes)]
                                   [id (components/RelationshipLine
                                         {:id id
                                          :connection c}
                                         {:from-pos-$ (:position-$ from)
                                          :to-pos-$ (:position-$ to)})]))
                               added)]
                      (merge lines (into {} new-lines))))
                  {}
                  (ulmus/zip
                    nodes-$
                    (ulmus/map (fn [[prev curr]]
                                 (clojure.set/difference curr prev))
                               (ulmus/slice 2 connections-$)))
                  (ulmus/map #(apply clojure.set/difference %) (ulmus/slice 2 connections-$)))]

    (ulmus/subscribe! connections-$ println)
    (ulmus/subscribe! lines-$ println)

    {:selected-nodes-$ (ulmus/start-with! #{} selected-nodes-$)
     :selected-resources-$ (ulmus/start-with! {} selected-resources-$)
     :recurrent/dom-$ (ulmus/map
                        (fn [[nodes-dom lines-dom]]
                          `[:div {:class "graffle-main"}
                            ^{:hipo/key "nodes"}
                            [:div {:class "nodes"}
                             ~@nodes-dom]
                            ^{:hipo/key "svg"}
                            [:svg
                             {}
                             ~@lines-dom]])
                        (ulmus/distinct
                          (ulmus/zip
                            (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals nodes-$))
                            (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals lines-$)))))
     :recurrent/state-$ (ulmus/signal-of (fn [] initial-state))}))

(defn start!
  []
  (recurrent/start!
    (state/with-state Graffle)
    {}
    {:recurrent/dom-$ (recurrent.drivers.vdom/for-id! "app")}))
  
