(ns konstellate.graffle.components
  (:require
    recurrent.drivers.rum
    ulmus.mouse
    [konstellate.graffle.util :as util]
    [konstellate.graffle.connections :as conn]
    [konstellate.components.core :as core-components]
    [recurrent.core :as recurrent :include-macros true]
    [ulmus.signal :as ulmus]))

(defmulti render-kind #(:kind (meta %)))

(defmethod render-kind :Deployment
  [resource])

(defn make-extents
  [client-rect]
  [(.-x client-rect)
   (.-y client-rect)
   (+
    (.-x client-rect)
    (.-width client-rect))
   (+
    (.-y client-rect)
    (.-height client-rect))])

(defn within?
  [[min-x min-y max-x max-y] [x y]]
  (and
    (>= x min-x)
    (>= y min-y)
    (<= x max-x)
    (<= y max-y)))

(recurrent/defcomponent Node
  [props sources]
  (let [id (gensym)
        held?-$ (ulmus/merge
                  (ulmus/map
                    (constantly true)
                    ((:recurrent/dom-$ sources) ".outline" "mousedown"))
                  (ulmus/map
                    (constantly false)
                    ((:recurrent/dom-$ sources) ".outline" "mouseup")))
        position-$ (ulmus/start-with!
                     [(rand 512) (rand 512)]
                     (ulmus/sample-when (:mouse-pos-$ sources) held?-$))
        connect-from-$ (ulmus/map
                         (constantly {:id (:id props)
                                      :type :connect-from
                                      :position-$ position-$})
                         ((:recurrent/dom-$ sources) :root "mousedown"))
        connect-to-$ (ulmus/map
                       (constantly {:id (:id props)
                                    :type :connect-to
                                    :position-$ position-$})
                       ((:recurrent/dom-$ sources) :root "mouseup"))
        dom-$ (ulmus/map (fn [[position selected content]]
                           (let [selected? (some #{(:id props)} selected)
                                 deselected? (and (not selected?)
                                                  (not (empty? selected)))]
                             [:div {:id (name (:id props))
                                    :class (str "node "
                                                (if selected? "selected ")
                                                (if deselected? "deselected"))
                                    :style 
                                    {:transform 
                                     (util/transform 
                                       "translate" ""
                                       (map #(str "calc(" % "px - 50%)")
                                            position))}}
                              [:div {:class (str "outline "
                                                 (if selected? "selected"))}]
                              [:div {:class "name"}
                               [:span (:kind content)]
                               (get-in content [:metadata :name])]]))
                         (ulmus/zip 
                           position-$
                           (:selected-nodes-$ sources)
                           (:content-$ sources)))]

    {:connect-$ (ulmus/merge connect-from-$ connect-to-$)
     :position-$ position-$
     :recurrent/dom-$ dom-$}))

(recurrent/defcomponent RelationshipLine
  [props sources]
  {:recurrent/dom-$
   (ulmus/map (fn [[selected-relations [x1 y1] [x2 y2]]]
                (let [connection-hash (hash
                                        (select-keys
                                          (:connection props)
                                          [:from :to]))
                      selected? (some #{(keyword (str connection-hash))} selected-relations)]
                  [:g {}
                   [:line
                    {:class "relationship-line"
                     :x1 x1 :y1 y1 :x2 x2 :y2 y2
                     :stroke (if selected? "#00a2ff" "#838383")
                     :stroke-width (if selected? 4 2)}]
                   [:line {:id connection-hash
                           :class "relationship-click-target"
                           :x1 x1 :y1 y1 :x2 x2 :y2 y2
                           :stroke "transparent"
                           :stroke-width 20}]]))
              (ulmus/zip
                (:selected-relations-$ sources)
                (:from-pos-$ sources)
                (:to-pos-$ sources)))})

(recurrent/defcomponent-1 {:recurrent/portal true} ConnectionModal
  [props sources]
  (let [connectables-$ (ulmus/map
                         (fn [[a b]]
                           (println "Recalc connectables!")
                           (conn/connectables a b))
                         (:resource-connections-$ sources))

        connectable-kvs-$ (ulmus/map #(reduce merge %)
                                     connectables-$)

        from-$ (ulmus/map first (:resource-connections-$ sources))
        to-$ (ulmus/map second (:resource-connections-$ sources))

        from-select (core-components/Select
                      {}
                      (assoc (select-keys sources [:recurrent/dom-$])
                             :label-$ (ulmus/map
                                        #(str 
                                           (:kind %) " - " 
                                           (get-in %
                                                   [:metadata :name]))
                                        from-$)
                             :options-$ 
                             (ulmus/map
                               (fn [connectables]
                                 (println "CONNECTABLES:" connectables)
                                 (keys connectables))
                               connectable-kvs-$)))

        to-select (core-components/Select
                    {}
                    (assoc (select-keys sources [:recurrent/dom-$])
                           :label-$ (ulmus/map
                                      #(str
                                         (:kind %) " - "
                                         (get-in %
                                                 [:metadata :name]))
                                      to-$)
                           :options-$ (ulmus/map
                                        (fn [[connectables from]]
                                          (println "from:" (str from))
                                          (println "conn:" (get (into {} 
                                                                 (map (fn [[k v]] [(:value k) v])
                                                                      connectables)) from))
                                          (get (into {} 
                                                     (map (fn [[k v]] [(:value k) v])
                                                          connectables))
                                               from))
                                        (ulmus/zip
                                          connectable-kvs-$
                                          (:value-$ from-select)))))

        connection-type-$ (ulmus/map
                            (fn [[connectables from to]] 
                              (some
                                (fn [c]
                                  (let [c-lookup (into {} (map (fn [[k v]] [(:value k) v]) c))]
                                    (when (c-lookup from)
                                      (:connection (meta c)))))
                                connectables))
                            (ulmus/zip
                              connectables-$
                              (:value-$ from-select)
                              (:value-$ to-select)))

        message-$ (ulmus/map :desc connection-type-$)

        content-$ (ulmus/map 
                    (fn [[[connecting-from connecting-to]
                          message from-select to-select]]
                      [:div {:class "connection-modal-content"}
                       [:div {:class "padded"}
                         [:h3 {} (str "Connect "
                                      (get-in connecting-from
                                              [:metadata :name])
                                      " with " 
                                      (get-in connecting-to
                                              [:metadata :name]))]
                         [:div {:class "picker"}
                          from-select
                          to-select]
                         [:p {} message]]
                       [:div {:class "banner"}
                        [:div {:class "connect button primary"}
                         "Connect"]]])
                    (ulmus/zip
                      (:resource-connections-$ sources)
                      message-$
                      (:recurrent/dom-$ from-select)
                      (:recurrent/dom-$ to-select)))

        connect-$ 
        (ulmus/sample-on 
          (ulmus/zip
            (:resource-connections-$ sources)
            connection-type-$
            (:value-$ from-select)
            (:value-$ to-select))
          ((:recurrent/dom-$ sources)
           ".button.connect" "click"))]

    (ulmus/subscribe! connectables-$ #(println "connectables" %))

    {:connect-$ connect-$
     :recurrent/dom-$ (ulmus/map (fn [content]
                                   [:div {:class "modal-underlay"}
                                    [:div {:class "modal"}
                                     [:h1 {} ""]
                                     content]])
                                 content-$)}))


