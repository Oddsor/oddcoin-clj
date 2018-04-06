# oddcoin

Learning Clojure and blockchain! 

Based on "Rolling your Own Blockchain in Haskell" by Michael Burge (http://www.michaelburge.us/2017/08/17/rolling-your-own-blockchain.html)

## Usage
Run the jar to create a blockchain with a length of 20

    $ java -jar oddcoin-0.1.0-standalone.jar [args]

## Examples
This example creates a blockchain with two blocks. We can see that only one of the transactions were actually added to the block because only the miner of the first block has been awarded oddcoins!

    (def blockchain (mine-on [(->transaction "oddsor" "guy2" 100), (->transaction "guy3" "guy4" 50)]
                             "oddsor"
                             (mine-on [] "oddsor" [[]])))
    => #'oddcoin.core/blockchain
    blockchain
    =>
    ({:block-header {:miner "oddsor",
                     :parent-hash "17c0940dda03952375a667f98ab90ead6e5408d9",
                     :nonce 0,
                     :mined-at #object[java.time.Instant 0x65d74d98 "2018-04-06T22:11:00.890Z"]},
      :block (#oddcoin.core.transaction{:from "oddsor", :to "guy2", :amount 100})}
     {:block-header {:miner "oddsor",
                     :parent-hash "addbc1b3216d2fdb33393ffbbbb6adff83d59e14",
                     :nonce 0,
                     :mined-at #object[java.time.Instant 0x5720af10 "2018-04-06T22:11:00.888Z"]},
      :block ()}
     [])

To see the account balances in the chain:

    (balances blockchain)
    => {"oddsor" 1900, "guy2" 100}