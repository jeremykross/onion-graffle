(ns onion-graffle.components
  (:require
    recurrent.drivers.dom
    ulmus.mouse
    [onion-graffle.util :as util]
    [onion-components.core :as onion-components]
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

(recurrent/defcomponent NewResourceModal
  [props sources]
  (let [type-input (onion-components/TextInput {:label "Type"} (select-keys sources [:recurrent/dom-$]))
        name-input (onion-components/TextInput {:label "Name"} (select-keys sources [:recurrent/dom-$]))]
    {:creation-requests-$ (ulmus/sample-on 
                            (ulmus/map (fn [[kind the-name]] {:kind kind
                                                              :metadata {:name the-name}})
                                       (ulmus/zip (:value-$ type-input) (:value-$ name-input)))
                            ((:recurrent/dom-$ sources) ".create.button" "click"))
     :recurrent/dom-$ (ulmus/map
                        (fn [[type-input-dom name-input-dom]]
                          [:div {:class "new-resource-modal"}
                           [:div {:class "content"}
                            [:h2 "New Resource"]
                            type-input-dom
                            name-input-dom]
                           [:div {:class "bottom-banner"}
                            [:button {:class "button"} "Cancel"]
                            [:button {:class "create button primary"} "Create"]]])
                        (ulmus/zip
                          (:recurrent/dom-$ type-input)
                          (:recurrent/dom-$ name-input)))}))

(recurrent/defcomponent Node
  [props sources]
  (let [id (gensym)
        dom-$ (ulmus/map (fn [[position euler content]]
                           [:div {:id id
                                  :class "node"
                                  :style (util/map->css 
                                           {:transform (util/translate position)})}
                            content])
                         (ulmus/zip ulmus.mouse/position-$
                                    (:euler-$ sources)
                                    (:content-$ sources)))
        client-rect-$ (ulmus/map (fn [_]
                                   (if-let [elem (.getElementById js/document id)]
                                     (make-extents (.getBoundingClientRect elem))
                                     []))
                                 dom-$)]

    {:recurrent/dom-$ dom-$}))

