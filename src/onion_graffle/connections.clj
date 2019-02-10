(ns onion-graffle.connections)

(defmacro
  defconnection
  [n [first-name second-name] from to & body]
  `(def ~n
     {:type ~(str n)
      :to ~to
      :from ~from
      :connected? (fn [a# b#]
                    (let [[~first-name ~second-name]
                          (onion-graffle.connections/ordered
                            ~from ~to a# b#)]
                      (when (and ~first-name ~second-name)
                        ~@body)))}))
