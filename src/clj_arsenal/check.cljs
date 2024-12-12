(ns clj-arsenal.check
  (:require-macros clj-arsenal.check)
  (:require
   [clj-arsenal.check.common :as common]))

(def !status common/!status)

(defn expect
  [f & args]
  (when-not (apply f args)
    (throw
      (ex-info
        "Expectation Unsatisfied"
        {::expect-fun (or (when (fn? f) (some-> f .-name demunge symbol)) f)
         ::expect-args args}))))
