(ns oddcoin.core
  (:gen-class)
  (:import (org.apache.commons.codec.digest DigestUtils)
           (java.time Instant))
  (:require [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]))

(s/def ::account string?)
(s/def ::from ::account)
(s/def ::to ::account)
(s/def ::amount nat-int?)
;(s/def ::hash #(= "java.security.MessageDigest$Delegate" (type %)))
(s/def ::hash string?)

(s/def ::transaction (s/keys :req-un [::from
                                      ::to
                                      ::amount]))

(s/def ::miner ::account)
(s/def ::parent-hash ::hash)
(s/def ::nonce pos-int?)
(s/def ::mined-at #(= Instant (type %)))
(s/def ::block-header (s/keys :req-un [::miner
                                       ::parent-hash
                                       ::nonce
                                       ::mined-at]))

(s/def ::block (s/coll-of ::transaction))

(s/def ::node (s/or
                :node (s/keys :req-un [::block-header ::block])
                :genesis (s/and coll?
                                empty?)))

(s/def ::blockchain (s/coll-of ::node))

(defn sha-hash
  [^String parent]
  (DigestUtils/sha1Hex (prn-str parent)))

(defn make-genesis
  []
  [])
(s/fdef make-genesis
        :ret ::node)

(defn difficulty [blockchain]
  (throw UnsupportedOperationException))

(defn desired-difficulty [blockchain]
  (throw UnsupportedOperationException))

(defn mine-on
  [transactions account parent]
  (let [valid-chain #(< (difficulty %) (desired-difficulty %))
        ts transactions]
    (loop [nonce 0]
      (let [candidate {:block-header {:miner       account
                                      :parent-hash (sha-hash parent)
                                      :mined-at    (Instant/now)}
                       :block        transactions}]
        (if (valid-chain candidate)
          candidate
          (recur (inc nonce)))))))
(s/fdef mine-on
        :args (s/cat :transactions ::block :account ::account :parent ::blockchain)
        :ret ::node)


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(st/instrument)