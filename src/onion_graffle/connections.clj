(ns onion-graffle.connections)

(defmacro defconnection
  [n data]
  `(def ~n ~data))
