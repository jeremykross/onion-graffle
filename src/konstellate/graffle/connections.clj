(ns konstellate.graffle.connections)

(defmacro with-order
  [desc fk]
  `(fn [from# to# & data#]
     (apply (~fk ~desc)
            (concat
              (konstellate.graffle.connections/ordered
                (:from ~desc)
                (:to ~desc)
                from# to#)
              data#))))
