(ns onion-graffle.connections)

(def PodSpec<->PersistentVolumeClaim 
  {:from "PodSpec"
   :to "PersistentVolumeClaim"
   :inputs {}
   :connected? (fn [a b])
   :disconnect (fn [a b])
   :connect (fn [a b inputs])})

(def Container<->Volume
  {:from "Container"
   :to "Volume"
   :inputs {}
   :connected? (fn [a b])
   :disconnect (fn [a b])
   :connect (fn [a b inputs])})


(def all
  [PodSpec<->PersistentVolumeClaim])
