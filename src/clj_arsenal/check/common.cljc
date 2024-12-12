(ns ^:no-doc clj-arsenal.check.common
  (:require
   [clj-arsenal.basis.protocols.chain]
   [clj-arsenal.check :as-alias check]
   #?(:clj [clojure.pprint :refer [pprint]]
      :cljs [cljs.pprint :refer [pprint]])))

(def ^:private ansi-term-codes
  {:reset "\u001B[0m"
   :red "\u001B[31m"})

(def !status (atom {}))

#?(:clj
   (defn default-reporter
     [report]
     (when (= (::check/status report) ::check/failure)
       (println
         (:red ansi-term-codes)
         "Check Failed" (::check/key report)
         (:reset ansi-term-codes))
         (pprint (::check/error report))))

   :cljs
   (let [has-devtools? (boolean (find-ns 'devtools.formatters.core))
         in-browser? (boolean (.-document js/globalThis))]
     (defn default-reporter
       [report]
       (when (= (::check/status report) ::check/failure)
         (cond
           has-devtools?
           (js/console.error "Check Failed " (::check/key report) "\n" (::check/error report))
           
           in-browser?
           (js/console.error
             "Check Failed " (pr-str (::check/key report)) "\n"
             (with-out-str (pprint (ex-data (::check/error report))))
             (.-stack (::check/error report)))
           
           :else
           (js/console.error
             (:red ansi-term-codes) "Check Failed " (pr-str (::check/key report)) (:reset ansi-term-codes)
             "\n"
             (with-out-str (pprint (ex-data (::check/error report))))
             (.-stack (::check/error report))))))))

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
