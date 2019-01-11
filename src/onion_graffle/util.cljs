(ns onion-graffle.util
  (:require
    clojure.set
    [clojure.string :as string]
    [ulmus.signal :as ulmus]))

(defn transform
  [which unit v]
  (let [[x y z opt] v]
    (cond
      (and z opt)
      (str which "3d(" x unit ", " y unit ", " z unit ", " opt ")")

      (and x y z)
      (str which "3d(" x unit ", " y unit ", " z unit ")")

      (and x y)
      (str which "(" x unit ", " y unit ")")

      :else
      (str which "(" x unit ")"))))

(defn translate
  [position]
  (transform "translate" "px" position))

(defn rotate
  [[rx ry rz]]
  (str
    (transform "rotateX" "" [rx]) " "
    (transform "rotateY" "" [ry]) " "
    (transform "rotateZ" "" [rz])))

(defn scale
  [factor]
  (transform "scale" "" factor))

(defn map->css
  [m]
  (string/join "; " (map (fn [[k v]] (str (name k) ": " v)) m)))

(defn transduce-state
  [& args]
  (let [{:keys [enter exit init]
         :or {enter (fn [acc [k v]] (assoc acc k v))
              exit (fn [acc [k v]] (dissoc acc k))
              init {}}} (apply hash-map args)]
    (fn [state-$]
      (ulmus/reduce (fn [acc state]
                      (let [enter-keys (clojure.set/difference (into #{} (keys state))
                                                        (into #{} (keys acc)))
                            exit-keys (clojure.set/difference (into #{} (keys acc))
                                                       (into #{} (keys state)))]
                        (let [entered (reduce enter acc (select-keys state enter-keys))
                              exited (reduce exit entered (select-keys state exit-keys))]
                          exited)))
                    init state-$))))
