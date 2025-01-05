A very simple inline testing library.

```clojure
(require '[clj-arsenal.check :refer [check when-check samp samps expect]])

(check ::my-bad-check
 (expect = 1 2))

(check ::my-good-check
 (let [i (samp :integer)]
   (expect = i i)))

(when-check
 (defn foo [x y] (+ x y)))

(check ::another-check
 (expect = (foo 1 2) (foo 2 1)))
```

- Checks are run immediately if enabled, otherwise they expand `nil`
- Enable checks by setting `:clj-arsenal.check/enabled` to true in your deps alias
- Include/exclude specific namespaces by setting `:clj-arsenal.check/ns-include-re`
  and/or `:clj-arsenal.check/ns-exclude-re` regexes in your deps alias.
- If a top-level form within a check evaluates to a chainable
  (i.e an async value, including js promises), then the check will wait for it to
  resolve.
- The reporter determines what happens with check results, the default one prints
  the check report to the console in a very basic way.  It can be customized by
  setting `:clj-arsenal.check/reporter` to the qualified symbol of a custom reporter
  function.
- Generators produce test values, accessible via `samp` (one value) or `samps`
  (multiple values).  Only `:integer` and `:string` generators are currently
  built-in.  Add more by adding `clj-arsenal/check/generators.edn` files to the
  classpath; which contain maps of `key -> qualified-generator-function-symnol`.
  Generator functions are called from Clojure (i.e the macro, not at runtime).
- Status of all checks is assoced into a map within the `clj-arsenal.check/!status`
  atom.  For CI tests, `add-watch` this atom to figure out when all checks have
  passed, or if any have failed.  The values of this map will be one of
  `clj-arsenal.check/failure`, `clj-arsenal.check/success`, or `clj-arsenal.check/pending`.

Currently this library works for Clojure and ClojureScript.  It can also be used
within mixed ClojureDart projects, but the checks will always expand to `nil`.  A
different approach needs to be used with ClojureDart, due to to the semantics
of Dart itself; but there are plans to try and make something work.
