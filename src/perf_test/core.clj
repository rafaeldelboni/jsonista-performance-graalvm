(ns perf-test.core
  (:require [cheshire.core :as cheshire]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [jsonista.core :as j]
            [jsonista.tagged :as jt])
  (:import (clojure.lang Keyword)
           (com.fasterxml.jackson.databind ObjectMapper))
  (:gen-class))

(set! *warn-on-reflection* true)

(defn json-data [size] (slurp (str "resources/json" size ".json")))
(defn edn-data [size] (cheshire/parse-string (json-data size)))

(defn encode-data-json [x] (json/write-str x))
(defn decode-data-json [x] (json/read-str x))

(defn encode-cheshire [x] (cheshire/generate-string x))
(defn decode-cheshire [x] (cheshire/parse-string x))

(defn encode-jsonista [x] (j/write-value-as-string x))
(defn decode-jsonista [x] (j/read-value x))

(let [mapper (j/object-mapper {:modules [(j/java-collection-module)]})]
  (defn encode-jsonista-fast [x] (.writeValueAsString mapper x))
  (defn decode-jsonista-fast [x] (.readValue mapper ^String x ^Class Object)))

(let [mapper (j/object-mapper
              {:encode-key-fn name
               :decode-key-fn keyword})]
  (defn encode-jsonista-fastk [x] (.writeValueAsString mapper x))
  (defn decode-jsonista-fastk [x] (.readValue mapper ^String x ^Class Object)))

(let [mapper (ObjectMapper.)]
  (defn encode-jackson [x] (.writeValueAsString mapper x))
  (defn decode-jackson [x] (.readValue mapper ^String x ^Class Object)))

(let [mapper (j/object-mapper
              {:modules [(jt/module
                          {:handlers {Keyword {:tag "!k"
                                               :encode jt/encode-keyword
                                               :decode keyword}}})]})]
  (defn encode-jsonista-map [x] (j/write-value-as-string x mapper))
  (defn decode-jsonista-map [x] (j/read-value x mapper)))

(def bench-env
  {:benchmarks [{:name :encode
                 :fn [encode-data-json
                      encode-cheshire
                      encode-jsonista
                      encode-jsonista-map
                      encode-jackson
                      encode-jsonista-fastk
                      encode-jsonista-fast]
                 :args [:state/edn]}
                {:name :decode
                 :fn [decode-data-json
                      decode-cheshire
                      decode-jsonista
                      decode-jsonista-map
                      decode-jackson
                      decode-jsonista-fastk
                      decode-jsonista-fast]
                 :args [:state/json]}]
   :params {:size ["10b" "100b" "1k" "10k" "100k"]}
   :states {:json {:fn json-data, :args [:param/size]}
            :edn {:fn edn-data, :args [:param/size]}}})

(defn -main [& _args]
  (let [{:keys [benchmarks params states]} bench-env
        inputs (map (fn [arg]
                      {:json {:arg arg
                              :data ((-> states :json :fn) arg)}
                       :edn {:arg arg
                             :data ((-> states :edn :fn) arg)}})
                    (:size params))
        data (doall
              (flatten
               (map (fn [bench]
                      (map (fn [fn-exec]
                             (map (fn [{:keys [arg data]}]
                                    {:type (:name bench)
                                     :fn (str fn-exec)
                                     :arg arg
                                     :time (let [exec-time (with-out-str
                                                             (time (fn-exec data)))
                                                 ran-time (str/split exec-time #" ")]
                                             (str (nth ran-time 2)
                                                  " "
                                                  (str/replace (nth ran-time 3) "\"\n" "")))})
                                  (case (:name bench)
                                    :encode (map :edn inputs)
                                    :decode (map :json inputs))))
                           (:fn bench)))
                    benchmarks)))]
    (println (encode-cheshire data))))
