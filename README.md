# oddcoin

Learning Clojure and blockchain! 

Based on "Rolling your Own Blockchain in Haskell" by Michael Burge (http://www.michaelburge.us/2017/08/17/rolling-your-own-blockchain.html)

## Usage
Run the jar to create a blockchain with a length of 20

    $ java -jar oddcoin-0.1.0-standalone.jar [args]

## Examples
This example creates a blockchain with two blocks. We can see that only one of the transactions were actually added to the block because only the miner of the first block has been awarded oddcoins!

    (def blockchain (mine-on [{:from "oddsor", :to "guy2", :amount 100}, {:from "guy3", :to "guy4", :amount 50}]
                             "oddsor"
                             (mine-on [] "oddsor" [[]])))
    => #'oddcoin.core/blockchain
    blockchain
    =>
    ({:block-header {:miner "oddsor",
                     :parent-hash "a0b1ca4775466d1e0129f9c5a69efa1ef612d814",
                     :nonce 0,
                     :mined-at #object[java.time.Instant 0x3e7e9232 "2018-04-06T21:52:38.493Z"]},
      :block ({:from "oddsor", :to "guy2", :amount 100})}
     {:block-header {:miner "oddsor",
                     :parent-hash "addbc1b3216d2fdb33393ffbbbb6adff83d59e14",
                     :nonce 0,
                     :mined-at #object[java.time.Instant 0x61962b47 "2018-04-06T21:52:38.489Z"]},
      :block ()}
     [])

To see the account balances in the chain:

    (balances blockchain)
    => {"oddsor" 1900, "guy2" 100}