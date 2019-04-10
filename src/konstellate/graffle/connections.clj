(ns konstellate.graffle.connections
  (:require 
    [clojure.data.json :as json]
    [clojure.string :as string]))

(defmacro with-order
  ([n desc fk] `(with-order ~n ~desc ~fk []))
  ([n desc fk default]
   `(fn [from# to# & data#]
      (let [order# 
            (konstellate.graffle.connections/ordered
              (:from ~desc)
              (:to ~desc)
              from# to#)]
      (with-meta 
        (apply 
          (fn [a# b# & data1#]
            (println "Calling: " ~fk)
            (if (not (and a# b# (~fk ~desc)))
              ~default
              (apply (~fk ~desc) a# b# data1#)))
          (concat
            order#
            data#))
        {:connection ~n})))))

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
