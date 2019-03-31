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
      (get-in outer path-to-inner))))

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
   :desc "The selector on this service matches the key/value pairs in the Pod or PodSpec."
   :connectables (fn [service pod]
                   {:from [[:labels]]
                    :to [[:selector]]})
   :connect (fn [service pod])
   :disconnect (fn [service pod])
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
   :connections (fn [config pod]
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
           [Service<->Pod
            ServiceAccount<->Pod
            ServiceAccount<->RoleBinding
            Role<->RoleBinding
            PersistentVolumeClaim<->Volume
            Config<->Volume
            Config<->Env])))


(defn possible
  [a b])
