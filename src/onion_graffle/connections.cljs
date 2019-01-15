(ns onion-graffle.connections
  (:require-macros [onion-graffle.connections :refer [defconnection]]))

(defconnection PodSpec<->PersistentVolumeClaim 
  {:inputs {}
   :connected? (fn [a b])
   :disconnect (fn [a b])
   :connect (fn [a b inputs])})

(def all
  [PodSpec<->PersistentVolumeClaim])
