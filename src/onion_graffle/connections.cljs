(ns onion-graffle.connections
  (:require-macros 
    [onion-graffle.connections :refer [defconnection]]))

(def paths
  {"PodTemplateSpec" {"Deployment" [:spec :template]}})

(defn has-all-of?
  [a b]
  (every? (fn [[k v]] (= (get b k) v)) a))

(defn get-kind-within
  [outer inner]
  (if (= (:kind outer) inner)
    outer
    (if-let [path-to-inner (get (paths inner) (:kind outer))]
      (get-in outer path-to-inner))))

(defn ordered
  [look-for-first look-for-second a b]
  (let [kind-within-a (partial get-kind-within a)
        kind-within-b (partial get-kind-within b)
        find-in (fn [look-for] (some #(or (kind-within-a %)
                                          (kind-within-b %)) look-for))]
    [(find-in look-for-first) (find-in look-for-second)]))
        

(defconnection Service<->Pod
  [service pod]
  ["Service"] ["PodTemplateSpec" "PodSpec" "Pod"]
  (has-all-of?
    (get-in service [:spec :selector])
    (get-in pod [:metadata :labels])))


(defn between
  [a b]
  (reduce (fn [acc c]
            (if ((:connected? c) a b)
              (conj acc
                    {:type (:type c)
                     :from (:key (meta a))
                     :to (:key (meta b))})
              acc))
          []
          [Service<->Pod]))

