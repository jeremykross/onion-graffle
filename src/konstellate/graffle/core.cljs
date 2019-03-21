(ns konstellate.graffle.core
  (:require
    clojure.set
    recurrent.drivers.rum
    recurrent.drivers.http
    recurrent.core
    [konstellate.graffle.components :as components]
    [konstellate.graffle.connections :as connections]
    [ulmus.mouse :as mouse]
    [ulmus.signal :as ulmus]
    [recurrent.core :as recurrent]
    [recurrent.state :as state]))

(def swagger-endpoint "https://raw.githubusercontent.com/kubernetes/kubernetes/master/api/openapi-spec/swagger.json")

(def initial-state
  {:foo {:kind "Deployment"
         :metadata {:name "foo"}
         :spec {:template {:spec {:containers [{:env [{:valueFrom {:configMapKeyRef {:name "baz"}}}]}]}
                           :metadata {:labels {:app "foobar"}}}}}
   :baz {:kind "ConfigMap"
         :metadata {:name "baz"}}
   :bar {:kind "Service"
         :metadata {:name "bar"}
         :spec {:selector {:app "foobar"}}}})

(recurrent.core/defcomponent Graffle
  [_ sources]
  (let [get-id-fn (fn [e]
                    (.stopPropagation e)
                    (keyword
                      (.getAttribute (.-currentTarget e) "id")))

        selected-node-id-$
        (ulmus/map
          get-id-fn
          (ulmus/merge
            ((:recurrent/dom-$ sources) ".node" "mousedown")
            ((:recurrent/dom-$ sources) ".node" "click")))

        selected-relation-id-$
        (ulmus/map
          get-id-fn
          (ulmus/merge
            ((:recurrent/dom-$ sources) ".relationship-click-target" "mousedown")
            ((:recurrent/dom-$ sources) ".relationship-click-target" "click")))

        selected-nodes-$ 
        (ulmus/merge
          (ulmus/map (constantly [])
                     (ulmus/merge
                       ((:recurrent/dom-$ sources) :root "click")
                       ((:recurrent/dom-$ sources) ".relationship-click-target" "click")))
          (ulmus/map vector selected-node-id-$)
          (:selected-nodes-$ sources))

        selected-relations-$
        (ulmus/merge
          (ulmus/map (constantly [])
                     (ulmus/merge
                       ((:recurrent/dom-$ sources) :root "click")
                       ((:recurrent/dom-$ sources) ".node" "click")))
          (ulmus/map vector selected-relation-id-$))

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
                          (into
                            {} (map
                                 (fn [[k r]]
                                   [k (components/Node
                                        {:id k}
                                        (assoc sources
                                               :content-$ (ulmus/map
                                                            #(get % k)
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
                            (println "Recalc:" state)

                            (let [with-key (into {} (map (fn [[k v]]
                                                           [k (with-meta v {:key k})]) state))
                                  to-check (map-indexed (fn [i [k v]]
                                                          [v (subvec (into [] (vals with-key)) i)])
                                                        with-key)
                                  conn
                                  (flatten
                                    (map (fn [[r others]]
                                           (map #(connections/between r %) others))
                                         to-check))]
                              (into #{} conn)))
                          (ulmus/distinct
                            (:recurrent/state-$ sources))))

        lines-$ (ulmus/reduce
                  (fn [lines change]
                    (if (:added (meta change))
                      (let [new-lines
                            (map (fn [c] 
                                   (let [id (gensym)
                                         from ((:from c) @nodes-$)
                                         to ((:to c) @nodes-$)]
                                     [id (components/RelationshipLine
                                           {:id id
                                            :connection c}
                                           {:selected-relations-$ selected-relations-$
                                            :from-pos-$ (:position-$ from)
                                            :to-pos-$ (:position-$ to)})]))
                                 change)]
                        (merge lines (into {} new-lines)))
                      lines))
                  {}
                  (ulmus/merge
                    (ulmus/map (fn [[prev curr]]
                                 (with-meta
                                   (clojure.set/difference curr prev)
                                   {:added true}))
                               (ulmus/slice 2 connections-$))
                    (ulmus/map #(with-meta (apply clojure.set/difference %)
                                           {:removed true})
                                           (ulmus/slice 2 connections-$))))]

    {:selected-nodes-$ (ulmus/start-with! #{} selected-nodes-$)
     :selected-resources-$ (ulmus/start-with! {} selected-resources-$)
     :selected-relations-$ (ulmus/start-with! #{} selected-relations-$)
     :swagger-$ (ulmus/signal-of [:get])
     :recurrent/dom-$ (ulmus/map
                        (fn [[nodes-dom lines-dom]]
                          `[:div {:class "graffle-main"}
                            [:div {:class "nodes"}
                             ~@nodes-dom]
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
    {:selected-nodes-$ (ulmus/signal-of [])
     :swagger-$ (recurrent.drivers.http/create!
                  swagger-endpoint
                  {:with-credentials? false})
     :recurrent/dom-$ (recurrent.drivers.rum/create! "app")}))
  

;(.addEventListener js/document "DOMContentLoaded" start!)
