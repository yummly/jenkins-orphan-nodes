(ns jenkins-orphan-nodes.core
  (:require [amazonica.aws.ec2 :as ec2]
            [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojurewerkz.propertied.properties :as p]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.edn :as edn]
            [amazonica.aws.s3 :as s3])
  (:import [com.offbytwo.jenkins JenkinsServer]
           [com.offbytwo.jenkins.model Computer ComputerWithDetails]
           [java.net URI]
           [org.joda.time DateTime]
           [com.amazonaws.services.lambda.runtime RequestStreamHandler Context]
           [java.io PushbackReader]))

(set! *warn-on-reflection* true)

(defn autoscaling-instance? [{:keys [tags]}]
  (some #(= (:key %) "aws:autoscaling:groupName") tags))

(defn instance-too-new? [{:keys [^DateTime launch-time]}]
  (let [now (System/currentTimeMillis)
        then (.getMillis launch-time)]
    (< (- now then) (* 5 60 1000))))

(defn find-running-instances [security-group]
  (->> (ec2/describe-instances :filters [{:name "instance-state-name" :values ["running"]}
                                         {:name "group-name" :values [security-group]}])
       :reservations
       (mapcat :instances)
       (remove autoscaling-instance?)
       (remove instance-too-new?)
       (map :instance-id)
       set))

(defn builder->instance-id [{:keys [displayName]}]
  (when-let [[_ id] (re-find #"\((i-[0-9a-f]+)\)" displayName)]
    id))

(defn find-builders [^String jenkins-url ^String user ^String password]
  (let [^JenkinsServer s (JenkinsServer. (URI. jenkins-url) user password)]
    (->> (.getComputers s)
         vals
         (map bean)
         (remove :offline)
         (keep builder->instance-id)
         set)))

(defn find-orphans [{:keys [security-group
                            jenkins-url user password]}]
  (let [instances (find-running-instances security-group)
        builders (find-builders jenkins-url user password)]
    (set/difference instances builders)))

(defmulti notify (fn [conf & _] (:type conf)))

(defmethod notify :slack
  [{:keys [url]} message]
  (let [{:keys [status error]} @(http/post url
                                           {:headers {"Content-Type" "application/json"}
                                            :body (json/write-str {:text message})})]))

(defn run-report [{:keys [security-group
                          jenkins-url user password
                          notification-config]
                   :as config}]
  (let [_ (printf "Running the Jenkins leaked builder detector with config %s\n"
                  (assoc config :password "XXX"))
        orphans (find-orphans config)
        _ (printf "Found leaked builders: %s\n" orphans)
        msg (if (seq orphans)
              (format "Found Jenkins builder instances Jenkins has forgotten about: %s" (clojure.string/join ", " orphans))
              "No leaked Jenkins builders found")]
    (notify notification-config msg)))

(defn config-properties []
  (p/load-from (io/resource "lambda.properties")))

(defn read-config [fn-name]
  (let [props (config-properties)
        bucket (get props "configBucket")
        prefix (get props "configPrefix")
        path (str prefix fn-name "/config.edn")
        ]
    (-> (s3/get-object bucket path)
        :input-stream
        io/reader
        PushbackReader.
        edn/read)))

(deflambdafn jenkins_orphan_nodes.core.LambdaFn
  [in out ctx]
  (let [f-n (.getFunctionName ^Context ctx)
        config (read-config f-n)]
    (run-report config)))
