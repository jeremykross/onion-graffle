(ns onion-graffle.connections)

(def PodSpec<->PersistentVolumeClaim 
  {:from "PodSpec"
   :to "PersistentVolumeClaim"
   :connected? (fn [a b])
   :connect (fn [a b])
   :disconnect (fn [a b])})

(def all
  [PodSpec<->PersistentVolumeClaim])
