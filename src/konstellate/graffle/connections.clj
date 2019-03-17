(ns konstellate.graffle.connections
  (:require 
    [clojure.data.json :as json]
    [clojure.string :as string]
    [clj-http.client :as http]))

(def swagger-endpoint "https://raw.githubusercontent.com/kubernetes/kubernetes/master/api/openapi-spec/swagger.json")
(def swagger (json/read-str (:body (http/get swagger-endpoint)) :key-fn keyword))

(defn name-only
  [full]
  (last (string/split (str full) #"\.")))

(def definitions
  (into {}
    (map (fn [[k v]]
           (let [new-k (keyword (name-only k))]
             [new-k v]))
         (:definitions swagger))))

(defmacro swagger-definitions [] swagger)

(defn kind-at-path-fn
  [outer-kind path]
  (loop [outer-kind outer-kind
         path path]
    (if (empty? path)
      (if outer-kind
        (keyword (name-only outer-kind)))
      (let [spec (get definitions outer-kind) 
            prop (first path)
            basic-type (get-in spec [:properties prop :type])
            next-kind (get-in spec [:properties prop :$ref])]
        (recur
          (or
            next-kind
            basic-type)
          (rest path))))))

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
