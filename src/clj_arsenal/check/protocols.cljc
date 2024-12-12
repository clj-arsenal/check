(ns clj-arsenal.check.protocols)

(defprotocol AsyncChain
  (async-chain-fn [async]))

(defn- default-async-chain-fn
  [context complete]
  (complete context))

(extend-protocol AsyncChain
  #?(:clj Object :cljs object)
  (async-chain-fn [_] default-async-chain-fn)
  
  nil
  (async-chain-fn [_] default-async-chain-fn))

#?(:cljs
   (extend-protocol AsyncChain
     js/Promise
     (async-chain-fn [p]
       (fn promise-async-chain-fn [context complete]
         (-> p
           (.catch
             (fn [error]
               (complete (assoc context :check.core/error error))))
           (.then
             (fn []
               (complete context))))))))
