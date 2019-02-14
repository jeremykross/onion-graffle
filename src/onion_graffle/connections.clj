(ns onion-graffle.connections)

(defmacro with-order
  [desc fk]
  `(fn [from# to# & data#]
     (apply (~fk ~desc)
            (concat
              (onion-graffle.connections/ordered
                (:from ~desc)
                (:to ~desc)
                from# to#)
              data#))))
