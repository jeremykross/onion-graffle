(ns konstellate.graffle.components
  (:require
    recurrent.drivers.vdom
    ulmus.mouse
    [konstellate.graffle.util :as util]
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
                           (let [selected? (some #{(:id props)} selected)]
                             [:div {:id (name (:id props))
                                    :class (str "node "
                                                (if selected? "selected"))
                                    :style (util/map->css 
                                             {:transform 
                                              (util/transform 
                                                "translate" ""
                                                (map #(str "calc(" % "px - 50%)")
                                                     position))})}
                              [:div {:class (str "outline "
                                                 (if selected? "selected"))}]
                              [:div {:class "name"}
                               content]]))
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
   (ulmus/map (fn [[[x1 y1] [x2 y2]]]
                [:line {:id (hash (:connection props)) :class "relationship-line" :x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "rgba(255,255,255,0.3)" :stroke-width 1}])
              (ulmus/zip (:from-pos-$ sources) (:to-pos-$ sources)))})

