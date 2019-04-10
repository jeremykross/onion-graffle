(ns konstellate.graffle.connections
  (:require
    [clojure.string :as string])
  (:require-macros 
    [konstellate.graffle.connections :refer [defconnection with-order]]))

(def paths
  {"PodTemplateSpec" {"Deployment" [:spec :template]
                      "ReplicationController" [:spec :template]}
   "PodSpec" {"Deployment" [:spec :template :spec]
              "ReplicationController" [:spec :template :spec]}})

(defn has-all-of?
  [a b]
  (and a b
       (every? (fn [[k v]] (= (get b k) v)) a)))

(defn get-kind-within
  [outer inner]
  (if (= (:kind outer) inner)
    outer
    (if-let [path-to-inner (get (paths inner) (:kind outer))]
      (with-meta 
        (get-in outer path-to-inner)
        (assoc (meta outer)
               :path-to-inner path-to-inner
               :outer outer)))))

(defn ordered
  [look-for-first look-for-second a b]
  (let [kind-within-a (partial get-kind-within a)
        kind-within-b (partial get-kind-within b)
        find-in (fn [look-for] (some #(or (kind-within-a %)
                                          (kind-within-b %)) look-for))]
    [(find-in look-for-first) (find-in look-for-second)]))
        

(defconnection Service<->Pod
  {:from ["Service"]
   :to ["PodTemplateSpec" "Pod"]
   :desc "Requests to this service will be forwarded to the pods on the associated workload."
   :connectables (fn [service pod]
                   {{:label "metadata.labels" 
                     :value "metadata.labels"}
                    [{:label "spec.selector"
                      :value "spec.selector"}]})
   :connect (fn [service pod]
              [(assoc-in service [:spec :selector]
                         (get-in pod [:metadata :labels]))
               (:outer (meta pod))])
   :connections (fn [service pod]
                  (if (has-all-of?
                        (get-in service [:spec :selector])
                        (get-in pod [:metadata :labels]))
                    [{
                      :desc "The selector on this service matches the key/value pairs in the Pod or PodSpec."
                      :info (get-in pod [:metadata :labels])}]))})

; does this work?
(defconnection Config<->Env
  {:from ["ConfigMap" "Secret"]
   :to ["PodSpec" "Pod"]
   :desc "A key/value in this ConfigMap or Secret's data will be exposed as an enivronment variable in one of the pods containers."
   :connect (fn [config pod]
              (println "Meta:" (meta config))
              [config (update-in (:outer (meta pod))
                                 (concat (:path-to-inner (meta pod))
                                         [:containers (:data (meta pod))])
                                 (fn [c]
                                   (assoc c :env [{:name (str (gensym))
                                                   :valueFrom {:configMapKeyRef (:data (meta config))}}])))])

   :connectables
   (fn [config pod]
     (let [containers (map-indexed
                        (fn [idx c]
                          {:label
                            (str 
                              (get-in c [:metadata :name])
                              " - container[" idx "]")
                            :value idx})
                        (:containers pod))
           kvs (map (fn [[k v]]
                      {:label
                       (str
                         (str (name k)) " : " (str (name v)))
                       :value {:name (get-in config [:metadata :name])
                               :key k}})
                    (:data config))]
       (into {}
             (map vector
                  containers
                  (repeat kvs)))))
   :connections (fn [config pod]
                  (let [config-name (get-in config [:metadata :name])
                        env (flatten (map :env (:containers pod)))
                        value-from (map :valueFrom env)]
                    (filter #(= (:name (or (:configMapKeyRef %)
                                           (:secretKeyRef %)))
                                config-name) value-from)))})

(defconnection Config<->Volume
  {:from ["ConfigMap" "Secret"]
   :to ["PodSpec" "Pod"]
   :desc "The values in the ConfigMap or Secret will be mounted into the Volume."
   :connectables
   (fn [config pod]
     (let [volumes (map (fn [v]
                          {:label (get-in v [:metadata :name])
                           :value v})
                        (:volumes pod))]
       (into 
         {}
         (map vector
              volumes
              (repeat [{:label (str (count (:data config)) " value(s)")
                        :value config}])))))
   :connections
   (fn [config pod]
     (filter
       (fn [v]
         (let [connected-name (or (get-in v [:secret :secretName])
                                  (get-in v [:configMap :name]))]
           (= connected-name (get-in config [:metadata :name]))))
       (:volumes pod)))})

(defconnection PersistentVolumeClaim<->Volume
  {:from ["PersistentVolumeClaim"]
   :to ["PodSpec" "Pod"]
   :connections (fn [pvc pod]
                  (filter (fn [v]
                            (= (get-in v [:persistentVolumeClaim :claimName])
                               (get-in pvc [:metadata :name])))
                          (:volumes pod)))})

(defconnection ServiceAccount<->Pod
  {:from ["ServiceAccount"]
   :to ["PodSpec" "Pod"]
   :connections (fn [service-account pod]
                  (if (= (:serviceAccountName pod) (get-in service-account [:metadata :name]))
                    [{}]
                    []))})

(defconnection Role<->RoleBinding
  {:from ["Role"]
   :to ["RoleBinding"]
   :connections (fn [role role-binding]
                  (if (= (get-in role-binding [:roleRef :name])
                         (get-in role [:metadata :name]))
                    [{}]
                    []))})

(defconnection ServiceAccount<->RoleBinding
  {:from ["ServiceAccount"]
   :to ["RoleBinding"]
   :connections (fn [service-account role-binding]
                  (filter (fn [subject]
                            (and (= (:kind subject) "ServiceAccount")
                                 (= (:name subject) (get-in service-account [:metadata :name]))
                                 (= (:namespace subject) (get-in service-account [:metadata :namespace]))))
                          (:subjects role-binding)))})

; StorageClass to volumeClaimTemplate


(def connections
  [Service<->Pod
   ServiceAccount<->Pod
   ServiceAccount<->RoleBinding
   Role<->RoleBinding
   PersistentVolumeClaim<->Volume
   Config<->Volume
   Config<->Env])

(defn between
  ([a b] (between nil a b))
  ([swagger a b]
   (reduce (fn [acc c]
             (let [connections ((:connections c) a b)]
               (concat acc
                       (map (fn [c] {:data c
                                     :from (:key (meta a))
                                     :to (:key (meta b))}) connections))))
           []
           connections)))

(defn connectables
  [a b]
  (reduce (fn [acc c]
            (let [connectables ((:connectables c) a b)]
              (conj acc connectables)))
          []
          connections))

(defn possible
  [a b])
