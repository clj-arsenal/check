(ns clj-arsenal.check)

;; nop macros and defs, to make it work in *.cljc files on hosts
;; for which the library isn't implemented

(defmacro  check
  [check-key & body]
  nil)

(defmacro when-check
  [& body]
  nil)

(defmacro samps
  [gen-key & {:as gen-opts}]
  nil)

(defmacro samp
  [gen-key & {:as gen-opts}]
  nil)

(defn expect
  [f & args]
  nil)
