(ns ^:no-doc clj-arsenal.check.generators
  (:require
   [clojure.string :as str])
  (:import
   java.util.Random))

(defn- rng-next-int
  [^Random rng min max]
  (+ min (rem (.nextLong rng) (- (inc max) min))))

(defn integer
  [& {:keys [max min limit seed] :or {min 0 max 2147483647 limit 32 seed 0}}]
  (let
    [rng (Random. seed)]
    (take limit
      (cond-> []
        (some? min) (conj min)
        (some? max) (conj max)
        true (into (repeatedly limit #(rng-next-int rng min max)))))))

(defn string
  [& {:keys [min-len max-len limit seed] :or {min-len 3 max-len 32 limit 32 seed 0}}]
  (let
    [rng (Random. seed)]
    (repeatedly limit
      (fn []
        (str/join
          (repeatedly
            (rng-next-int rng min-len max-len)
            #(char (rng-next-int rng 32 127))))))))
