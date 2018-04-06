(ns oddcoin.core
  (:gen-class)
  (:import (org.apache.commons.codec.digest DigestUtils)
           (java.time Instant)
           (java.math RoundingMode MathContext))
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
(s/def ::nonce nat-int?)
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

(s/def ::block-chain (s/coll-of ::node))

(defn sha-hash
  [^String block-chain]
  (DigestUtils/sha1Hex (prn-str block-chain)))

(defn make-genesis
  []
  [])
(s/fdef make-genesis
        :ret ::node)

(defn difficulty [^String blockchain-hash]
  (BigDecimal. (BigInteger. blockchain-hash 16)))

(def genesis-block-difficulty 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF)
(def genesis-decimal (BigDecimal. (BigInteger. (str genesis-block-difficulty))))
(def target-time 10)
(def num-blocks-to-calc-difficulty 100)

(defn block-time-average [block-chain]
  (let [mine-timestamps (take
                          num-blocks-to-calc-difficulty
                          (map #(.getEpochSecond %) (filter some? (map #(:mined-at (:block-header %)) block-chain))))
        zipped (map - mine-timestamps (rest mine-timestamps))
        count (count zipped)]
    (/ (reduce + zipped) (if (= 0 count) 1 count))))        ; Avoid division by 0

(defn adjustment-factor [block-chain]
  (let [avg (block-time-average block-chain)]
    (min 4.0
         (/ target-time (if (= 0 avg) 1 avg)))))            ; Avoid division by 0

(defn desired-difficulty [block-chain]
  (.round (loop [chain block-chain
                 accumulated-divisor (BigDecimal. "1")]
            (if (empty? (first chain))
              (.divide genesis-decimal accumulated-divisor 10000 RoundingMode/HALF_EVEN)
              (recur (rest chain) (.multiply (BigDecimal. (float (adjustment-factor chain))) accumulated-divisor)))) ; TODO trampoline this to avoid stackyflow
          MathContext/DECIMAL32))

(def block-reward 1000)

(s/def ::amount int?)
(s/def ::balances map?)

(defn balances [block-chain]
  (reduce
    (fn [map1 map2]
      (update map1
              (:account map2)
              (fn [old-balance] (+ (or old-balance 0) (or (:amount map2) 0)))))
    {}
    (flatten (filter
               some?
               (map
                 #(if-let
                    [header (:block-header %)]
                    (conj
                      (flatten
                        (map (fn [transaction]
                               [{:account (:from transaction) :amount (- (:amount transaction))}
                                {:account (:to transaction) :amount (:amount transaction)}])
                             (:block %)))
                      {:account (:miner header) :amount block-reward}))

                 block-chain)))))
(s/fdef balances
        :args (s/cat :block-chain ::block-chain)
        :ret ::balances)


(defn valid-transactions [transactions block-chain]
  (let [balances (balances block-chain)]
    (filter (fn [transaction]
              (and
                (> (:amount transaction) 0)
                (>=
                  (- (or (get balances (:from transaction)) -1) (:amount transaction))
                  0)))
            transactions)))

(defn mine-on
  [transactions account parent]
  (let [parent-hash (sha-hash parent)
        desired-difficulty (desired-difficulty parent)
        valid-chain #(let [diff (difficulty (sha-hash %))
                           compare (.compareTo diff desired-difficulty)]
                       (neg-int? compare))
        ts (valid-transactions transactions parent)]
    (loop [nonce 0]
      (let [candidate (concat [{:block-header {:miner       account
                                               :parent-hash parent-hash
                                               :nonce       nonce
                                               :mined-at    (Instant/now)}
                                :block        ts}]
                              parent)]
        (if (valid-chain candidate)
          candidate
          (recur (inc nonce)))))))
(s/fdef mine-on
        :args (s/cat :transactions ::block :account ::account :parent ::block-chain)
        :ret ::block-chain)

(defn -main
  "Mine a block-chain"
  [& args]
  (loop [chain [[]]
         times 20]
    (if (> times 0)
      (recur (mine-on [] "oddsor" chain) (dec times))
      chain)))

(st/instrument)