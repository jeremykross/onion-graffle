(ns konstellate.graffle.core
  (:require
    clojure.set
    recurrent.drivers.rum
    recurrent.drivers.http
    recurrent.core
    [konstellate.components.core :as core-components]
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
         :spec {:template {:spec {:volumes [{:metadata {:name "My Volume"}}]
                                  :containers [{:metadata {:name "My Container"}}]}
                           :metadata {:labels {:app "foobar"}}}}}
   :baz {:kind "ConfigMap"
         :metadata {:name "baz"}
         :data {:fizz "buzz"
                :foo "bar"}}
   :bar {:kind "Service"
         :metadata {:name "bar"}}})

(recurrent.core/defcomponent Graffle
  [props sources]
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


        ; need to id these based on from-to?
        connections-$ (ulmus/distinct
                        (ulmus/map
                          (fn [state]
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

        selected-connections-$ (ulmus/map
                                 (fn [[selected-relations connections]]
                                   (let [connection-hash-fn #(keyword (str (hash (select-keys % [:to :from]))))]
                                     (filter (fn [c]
                                               (some #{(connection-hash-fn c)} selected-relations))
                                             connections)))
                                 (ulmus/zip
                                   selected-relations-$
                                   connections-$))

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
                                           (ulmus/slice 2 connections-$))))

    creation-line-$ (ulmus/map
                      (fn [connection]
                        (if (= (:type connection) :connect-from)
                          (components/RelationshipLine
                            {:id :connection-line
                             :connection {}}
                            {:selected-relations-$ selected-relations-$
                             :from-pos-$ (:position-$ connection)
                             :to-pos-$ mouse-pos-$})
                          {:recurrent/dom-$ (ulmus/signal-of [])}))
                      (ulmus/merge
                        (ulmus/map 
                          (constantly {:type :break})
                          ((:recurrent/dom-$ sources) :root "mouseup"))
                        (ulmus/pickmerge :connect-$ (ulmus/distinct (ulmus/map vals nodes-$)))))
    connection-requests-$ (ulmus/pickmerge :connect-$ (ulmus/distinct (ulmus/map vals nodes-$)))

    valid-connections-$ 
    (ulmus/filter (fn [[from to]]
                    (and 
                      (= (:type from) :connect-from)
                      (= (:type to) :connect-to)
                      (not= (:id from) (:id to))))
                  (ulmus/slice 2 connection-requests-$))

    resource-connections-$
    (ulmus/map
      (fn [[state c]]
        (map #(with-meta (get state (:id %)) (select-keys % [:id])) c))
      (ulmus/zip
        (:recurrent/state-$ sources)
        valid-connections-$))


    modal (components/ConnectionModal {} (assoc (select-keys sources [:recurrent/dom-$])
                                                :resource-connections-$ resource-connections-$))

    modal-shown?-$ 
    (ulmus/merge
      (ulmus/map
        (constantly false)
        (:connect-$ modal))
      (ulmus/map
        (constantly true)
        (ulmus/filter #(not (empty? %)) (:connectable-kvs-$ modal))))]

    {:connections-requests-$ (ulmus/pickmerge :connect-$ (ulmus/distinct (ulmus/map vals nodes-$)))
     :selected-nodes-$ (ulmus/start-with! #{} selected-nodes-$)
     :selected-resources-$ (ulmus/start-with! {} selected-resources-$)
     :selected-relations-$ (ulmus/start-with! #{} selected-relations-$)
     :selected-connections-$ selected-connections-$
     :swagger-$ (ulmus/signal-of [:get])
     :recurrent/dom-$ (ulmus/map
                        (fn [[modal modal-shown? creation-line-dom nodes-dom lines-dom]]
                          `[:div {:class "graffle-main"}
                            ~(if modal-shown? modal)
                            [:div {:class "nodes"}
                             ~@nodes-dom]
                            [:svg
                             {}
                             ~creation-line-dom
                             ~@lines-dom]])
                        (ulmus/distinct
                          (ulmus/zip
                            (:recurrent/dom-$ modal)
                            modal-shown?-$
                            (ulmus/pickmap :recurrent/dom-$ creation-line-$)
                            (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals nodes-$))
                            (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals lines-$)))))
     :recurrent/state-$ 
     (ulmus/merge
       (ulmus/signal-of (fn [] (or (:initial-state props) initial-state)))
       (ulmus/map (fn [[[from to] connection from-data to-data]]
                    (println "CONNECTION:" connection)
                    (println (meta from))
                    (fn [state]
                      (let [[connected-from connected-to] ((:connect connection)
                                                           (vary-meta from assoc :data from-data)
                                                           (vary-meta to assoc :data to-data))]
                        (-> state
                            (assoc (:id (meta connected-from)) connected-from)
                            (assoc (:id (meta connected-to)) connected-to)))))
                  (:connect-$ modal)))}))

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
