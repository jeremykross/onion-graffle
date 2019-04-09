(ns konstellate.graffle.connections
  (:require 
    [clojure.data.json :as json]
    [clojure.string :as string]))

(defmacro with-order
  ([n desc fk] `(with-order ~n ~desc ~fk []))
  ([n desc fk default]
   `(fn [from# to# & data#]
      (with-meta 
        (apply 
          (fn [a# b#]
            (if (not (and a# b#))
              ~default
              ((~fk ~desc) a# b#)))
          (concat
            (konstellate.graffle.connections/ordered
              (:from ~desc)
              (:to ~desc)
              from# to#)
            data#))
        {:connection ~n}))))

(defmacro make-connection
  [n conn]
  `(let [conn# ~conn]
     (assoc conn#
            :connections (with-order ~n conn# :connections)
            :connectables (with-order ~n conn# :connectables {})
            :connect (with-order ~n conn# :connect)
            :disconnect (with-order ~n conn# :disconnect))))

(defmacro defconnection
  [n conn]
  `(def ~n
     (make-connection 
       ~n
       (assoc ~conn :title ~(str n)))))
