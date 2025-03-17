(ns clj-arsenal.check
  (:require
   [clojure.repl :as repl]
   [clj-arsenal.check.common :as common]
   [clj-arsenal.check.macro-common :as macro-common]
   [clj-arsenal.basis :as b]))

(defmacro samps
  [gen-key & {:as gen-opts}]
  (binding [macro-common/*expand-host* (if (:ns &env) :cljs :clj)]
    (macro-common/expand-samps gen-key gen-opts)))

(defmacro samp
  [gen-key & {:as gen-opts}]
  (binding [macro-common/*expand-host* (if (:ns &env) :cljs :clj)]
    (macro-common/expand-samp gen-key gen-opts)))

(defmacro check
  [check-key & body]
  (binding [macro-common/*expand-host* (if (:ns &env) :cljs :clj)]
    (when (macro-common/check-ns? (str *ns*))
      (macro-common/expand-check check-key body))))

(defmacro when-check
  [& body]
  (binding [macro-common/*expand-host* (if (:ns &env) :cljs :clj)]
    (when (macro-common/check-ns? (str *ns*))
      `(do ~@body))))

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
        ::expect-fun (cond-> (str f) (fn? f) repl/demunge)
        ::expect-args args))))
