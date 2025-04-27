(ns clj-arsenal.check.macro-common
  (:require
    [clj-arsenal.basis :as b]
    [clojure.edn :as edn]
    [clojure.walk :as walk]
    [clojure.java.io :as io]
    [clj-arsenal.check.common :as common]
    [clj-arsenal.check :as-alias check])
  (:import
    java.net.URL
    java.io.PushbackReader))

(def ^:private enabled (boolean (b/get-in-config [::check/enabled] false)))
(def ^:private reporter (b/get-in-config [::check/reporter] `common/default-reporter))
(def ^:private include-ns-regex (some-> (b/get-in-config [::check/ns-include-re]) re-pattern))
(def ^:private exclude-ns-regex (some-> (b/get-in-config [::check/ns-exclude-re]) re-pattern))
(def ^:private gen-seed (some-> (b/get-in-config [::check/gen-seed] 0)))

(defn check-ns?
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

(def ^:dynamic *expand-host* nil)

(defn expand-samps
  [gen-key gen-opts]
  ;; cljd seems to fk with *reader-resolver* and break things
  (binding [*reader-resolver* nil]
    (walk/postwalk
      (fn [x] (cond->> x (seq? x) (cons 'list)))
      ((requiring-resolve (generators gen-key)) (merge {:seed gen-seed} gen-opts)))))

(defn expand-samp
  [gen-key gen-opts]
  `(clojure.core/first ~(expand-samps gen-key (assoc (merge gen-opts {:seed gen-seed}) :limit 1))))

(defn expand-chain-forms
  [forms context callback]
  (if (empty? forms)
    `(~callback ~context)
    (let [context-sym (gensym "context")]
      `(let [~context-sym ~context]
         (b/chain
           (clj-arsenal.basis/m
            ~(first forms)
            :catch b/err-any error#
            error#)
           (fn [next-value#]
             (if (b/err? next-value#)
               (~callback (assoc ~context-sym ::check/error next-value#))
               ~(expand-chain-forms (rest forms) context-sym callback))))))))

(defn expand-check
  [check-key body]
  `(do
     (swap! check/!status assoc ~check-key ::check/pending)
     ~(expand-chain-forms
        body
        `{::check/key ~check-key

          ::check/reporter
          ~(case *expand-host*
             :cljd reporter
             :cljs `(clojure.core/resolve '~reporter)
             :clj `(clojure.core/requiring-resolve '~reporter))}
        `(fn [final-context#]
           (common/report final-context#)))
     nil))
