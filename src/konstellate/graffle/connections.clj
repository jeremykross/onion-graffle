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
  [conn n desc]
  `(assoc ~conn
          :connections 
          (fn [from# to#]
            (map
              (fn [c#] (assoc c# 
                              :desc ~desc
                              :title ~n))
              ((with-order ~conn :connections)
               from# to#)))
          :connect (with-order ~conn :connect)
          :disconnect (with-order ~conn :disconnect)))

(defmacro defconnection
  [n desc conn]
  `(def ~n
     (make-connection ~conn ~(str n) desc)))
