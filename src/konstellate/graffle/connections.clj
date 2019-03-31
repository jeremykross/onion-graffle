(ns konstellate.graffle.connections
  (:require 
    [clojure.data.json :as json]
    [clojure.string :as string]))

(defmacro with-order
  ([desc fk] `(with-order ~desc ~fk []))
  ([desc fk default]
  `(fn [from# to# & data#]
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
         data#)))))

(defmacro make-connection
  [conn n]
  `(assoc ~conn
          :connections 
          (fn [from# to#]
            (map
              (fn [c#] (assoc c# 
                              :title ~n))
              ((with-order ~conn :connections)
               from# to#)))
          :connectables (with-order ~conn :connectables {:from [] :to []})
          :connect (with-order ~conn :connect)
          :disconnect (with-order ~conn :disconnect)))

(defmacro defconnection
  [n conn]
  `(def ~n
     (make-connection 
       ~conn
       ~(str n))))
