(defproject jenkins-orphan-nodes "0.1.0-SNAPSHOT"
  :description "Find orphaned Jenkins nods"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [uswitch/lambada "0.1.0"]
                 [amazonica "0.3.44"]
                 [com.offbytwo.jenkins/jenkins-client "0.3.3"]
                 [http-kit "2.1.18"]
                 ;; smaller than cheshire; we don't care about speed
                 [org.clojure/data.json "0.2.6"]
                 [clojurewerkz/propertied "1.2.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
