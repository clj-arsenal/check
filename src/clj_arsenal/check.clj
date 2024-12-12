(ns clj-arsenal.check
  (:require
   [clojure.java.io :as io]
   [clojure.walk :as walk]
   [clojure.repl :as repl]
   [clojure.edn :as edn]
   [clj-arsenal.check.common :as common]
   [clj-arsenal.check.protocols :as protocols])
  (:import
   java.net.URL
   java.io.PushbackReader))

(def ^:private deps
  (some-> (requiring-resolve 'clojure.java.basis/current-basis)
    (apply nil)
    :argmap))

(def ^:private enabled
  (boolean (::enabled deps)))

(def ^:private reporter
  (or
    (let [sym (::reporter deps)]
      (when (qualified-symbol? sym)
        sym))
    `common/default-reporter))

(def ^:private include-ns-regex (some-> (::ns-include-re deps) re-pattern))
(def ^:private exclude-ns-regex (some-> (::ns-exclude-re deps) re-pattern))

(defn- check-ns?
  [s]
  (and enabled
    (or (nil? include-ns-regex) (re-matches include-ns-regex s))
    (or (nil? exclude-ns-regex) (not (re-matches exclude-ns-regex s)))))

(def ^:private generators
  (reduce
    (fn [g ^URL url]
      (merge g
        (with-open [rdr (io/reader url)]
          (edn/read (PushbackReader. rdr)))))
    {}
    (-> (Thread/currentThread)
      .getContextClassLoader
      (.getResources "clj_arsenal/check/generators.edn")
      enumeration-seq)))

(defmacro samps
  [gen-key & {:as gen-opts}]
  (walk/postwalk
    (fn [x] (cond->> x (seq? x) (cons 'list)))
    ((requiring-resolve (generators gen-key)) gen-opts)))

(defmacro samp
  [gen-key & {:as gen-opts}]
  `(clojure.core/first (samps ~gen-key ~(assoc gen-opts :limit 1))))

(defmacro ^:no-doc async-chain-forms
  [forms context callback]
  (if (empty? forms)
    `(~callback ~context)
    `((protocols/async-chain-fn
        (common/try-catch
          (fn [] ~(first forms))
          (fn [error#] (common/fail error#))))
      ~context
      (fn [next-context#]
        (if (::error next-context#)
          (~callback next-context#)
          (async-chain-forms ~(rest forms) next-context# ~callback))))))

(defmacro check
  [check-key & body]
  (when (check-ns? (str *ns*))
    `(do
       (swap! common/!status assoc ~check-key ::pending)
       (async-chain-forms ~body
         {::key ~check-key
          ::reporter ~(if &env
                        `(clojure.core/resolve '~reporter)
                        `(clojure.core/requiring-resolve '~reporter))}
         (fn [final-context#]
           (common/report final-context#)))
       nil)))

(defmacro when-check
  [& body]
  (when (check-ns? (str *ns*))
    `(do ~@body)))

(def !status common/!status)

(defn expect
  [f & args]
  (when-not (apply f args)
    (throw
      (ex-info
        "Expectation Unsatisfied"
        {::expect-fun (cond-> (str f) (fn? f) repl/demunge)
         ::expect-args args}))))
