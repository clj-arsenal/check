(ns ^:no-doc clj-arsenal.check.common
  (:require
   [clj-arsenal.log :refer [log]]
   [clj-arsenal.basis :as b]
   [clojure.string :as str]))

(defonce !status (atom {}))

(defn default-reporter
  [report]
  (when (= (:clj-arsenal.check/status report) :clj-arsenal.check/failure)
    (log :error :msg "Check Failed" :ex (:clj-arsenal.check/error report))))

(defn report
  [context]
  (let [status (if (some? (:clj-arsenal.check/error context)) :clj-arsenal.check/failure :clj-arsenal.check/success)]
    ((:clj-arsenal.check/reporter context)
     (cond->
       {:clj-arsenal.check/status status
        :clj-arsenal.check/key (:clj-arsenal.check/key context)}
       (= :clj-arsenal.check/failure status)
       (assoc :clj-arsenal.check/error (:clj-arsenal.check/error context))))
    (swap! !status assoc (:clj-arsenal.check/key context) status)))

(defn await-all-checks
  []
  (b/chainable
    (fn [continue]
      (let
        [watch-key (gensym)
         
         on-status
         (fn [status]
           (let
             [{passed :clj-arsenal.check/success
               failed :clj-arsenal.check/failure
               pending :clj-arsenal.check/pending}
              (group-by val status)]
             (when (empty? pending)
               (remove-watch !status watch-key)
               (continue {:clj-arsenal.check/passed (map key passed) :clj-arsenal.check/failed (map key failed)}))))]
        (add-watch !status watch-key (fn [_ _ _ status] (on-status status)))
        (on-status @!status)))))

(defn report-all-checks-and-exit!
  []
  (b/chain
    (await-all-checks)
    (fn [{passed :clj-arsenal.check/passed failed :clj-arsenal.check/failed :as x}]
      (cond
        (seq failed)
        (do
          (print
            "The following checks failed:\n"
            (str/join "\n" (map #(str "  " %) failed))
            "\n")
          #?(:clj (System/exit 1) :cljs (js/process.exit 1)))
        
        :else
        (do
          (print "All " (count passed) " checks passed.")
          #?(:clj (System/exit 0) :cljs (js/process.exit 0)))))))
