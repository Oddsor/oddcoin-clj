(defproject oddcoin "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [orchestra "2017.11.12-1"]
                 [commons-codec "1.11"]]
  :main ^:skip-aot oddcoin.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
