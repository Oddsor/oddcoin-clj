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
(def target-time 10)
(def num-blocks-to-calc-difficulty 100)

(defn block-time-average [block-chain]
  (let [mine-timestamps (take
                          num-blocks-to-calc-difficulty
                          (map #(.toEpochMilli ^Instant (:mined-at (:block-header %))) block-chain))
        zipped (map - mine-timestamps (rest mine-timestamps))]
    (/ (reduce + zipped) (count zipped))))

(defn adjustment-factor [block-chain]
  (min 4.0
       (/
         target-time
         (block-time-average block-chain))))

(defn loopy [block-chain]
  (if (or (= nil (first block-chain)) (empty (first block-chain)))
    genesis-block-difficulty
    (fn [] (/ (loopy (rest block-chain)) (adjustment-factor block-chain)))))

(defn desired-difficulty [block-chain]
  (BigDecimal. ^Double (Math/rint
                         (trampoline loopy block-chain))))

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
        valid-chain #(let [diff (difficulty (sha-hash %))   ;TODO something fishy here
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
  "I don't do a whole lot ... yet."
  [& args]
  (loop [chain []
         times 4]
    (if (> times 0)
      (recur (mine-on [] "oddsor" chain) (dec times))
      chain)))

(st/instrument)