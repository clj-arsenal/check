(ns clj-arsenal.check
  (:require-macros clj-arsenal.check)
  (:require
   [clj-arsenal.check.common :as common]
   [clj-arsenal.basis :as b]))

(def !status common/!status)
(def await-all-checks common/await-all-checks)
(def report-all-checks-and-exit! common/report-all-checks-and-exit!)

(defn expect
  [f & args]
  (when-not (apply f args)
    (throw
      (b/err
        :p ::expectation-unsatisfied
        :msg "Expectation Unsatisfied"
        ::expect-fun (or (when (fn? f) (some-> f .-name demunge symbol)) f)
        ::expect-args args))))
