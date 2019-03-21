(ns konstellate.graffle.connections
  (:require 
    [clojure.data.json :as json]
    [clojure.string :as string]))

(defmacro with-order
  [desc fk]
  `(fn [from# to# & data#]
     (apply 
       (fn [a# b#]
         (if (not (and a# b#))
           []
           ((~fk ~desc) a# b#)))
       (concat
         (konstellate.graffle.connections/ordered
           (:from ~desc)
           (:to ~desc)
           from# to#)
         data#))))

(defmacro make-connection
  [desc]
  `(assoc ~desc
          :connections (with-order ~desc :connections)
          :connect (with-order ~desc :connect)
          :disconnect (with-order ~desc :disconnect)))

(defmacro defconnection
  [n desc]
  `(def ~n
     (make-connection ~(assoc desc
                              :type (str n)))))
