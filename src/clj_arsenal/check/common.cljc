(ns ^:no-doc clj-arsenal.check.common
  (:require
   [clj-arsenal.check :as-alias check]
   [clj-arsenal.log :refer [log]]
   [clj-arsenal.basis :as b]
   [clojure.string :as str]))

(def !status (atom {}))

(defn default-reporter
  [report]
  (when (= (::check/status report) ::check/failure)
    (log :error :msg "Check Failed" :ex (::check/error report))))

(defn report
  [context]
  (let [status (if (some? (::check/error context)) ::check/failure ::check/success)]
    ((::check/reporter context)
     (cond->
       {::check/status status
        ::check/key (::check/key context)}
       (= ::check/failure status)
       (assoc ::check/error (::check/error context))))
    (swap! !status assoc (::check/key context) status)))

(defn await-all-checks
  []
  (b/chainable
    (fn [continue]
      (let
        [watch-key (gensym)
         
         on-status
         (fn [status]
           (let
             [{passed ::check/success failed ::check/failure pending ::check/pending}
              (group-by val status)]
             (when (empty? pending)
               (remove-watch !status watch-key)
               (continue {::check/passed (map key passed) ::check/failed (map key failed)}))))]
        (add-watch !status watch-key (fn [_ _ _ status] (on-status status)))
        (on-status @!status)))))

(defn report-all-checks-and-exit!
  []
  (b/chain
    (await-all-checks)
    (fn [{passed ::check/passed failed ::check/failed :as x}]
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
