(ns konstellate.graffle.connections
  (:require-macros 
    [konstellate.graffle.connections :refer [with-order]]))

(def paths
  {"PodTemplateSpec" {"Deployment" [:spec :template]}})

(defn has-all-of?
  [a b]
  (and a b
       (every? (fn [[k v]] (= (get b k) v)) a)))

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
        

(defn make-connection
  [desc]
  (assoc desc
         :connected? (with-order desc :connected?)
         :connect (with-order desc :connect)
         :disconnect (with-order desc :disconnect)))

(def Service<->Pod (make-connection
                     {:from ["Service"]
                      :to ["PodTemplateSpec" "PodSpec" "Pod"]
                      :connected? (fn [service pod]
                                    (has-all-of?
                                      (get-in service [:spec :selector])
                                      (get-in pod [:metadata :labels])))
                      :connect (fn [service pod]
                                 [(assoc-in service [:spec :selector]
                                            (get-in pod [:metadata :labels]))
                                  pod])
                      :disconnect (fn [service pod]
                                    [(update-in service [:spec :selector]
                                                (fn [selector]
                                                  (apply dissoc selector (keys (get-in pod [:metadata :labels])))))
                                     pod])}))

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

