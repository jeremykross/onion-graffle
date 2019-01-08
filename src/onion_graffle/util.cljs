(ns onion-graffle.util
  (:require
    [clojure.string :as string]))

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


