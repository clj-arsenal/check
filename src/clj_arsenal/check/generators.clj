(ns ^:no-doc clj-arsenal.check.generators
  (:require
   [clojure.string :as str]))

(defn- rand-int'
  [min max]
  (+ min (rand-int (- (inc max) min))))

(defn integer
  [& {:keys [max min limit] :or {min 0 max 2147483647 limit 32}}]
  (take limit
    (cond-> []
      (some? min) (conj min)
      (some? max) (conj max)
      true (into (repeatedly limit #(rand-int' min max))))))

(defn string
  [& {:keys [min-len max-len limit] :or {min-len 0 max-len 32 limit 32}}]
  (repeatedly limit
    (fn []
      (str/join
        (repeatedly
          (rand-int' min-len max-len)
          #(char (rand-int' 32 127)))))))
