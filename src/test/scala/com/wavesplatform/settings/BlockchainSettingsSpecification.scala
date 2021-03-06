package com.wavesplatform.settings

import java.io.File

import com.typesafe.config.ConfigFactory
import com.wavesplatform.state2.ByteStr
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class BlockchainSettingsSpecification extends FlatSpec with Matchers {
  "BlockchainSettings" should "read custom values" in {
    val config = ConfigFactory.parseString(
      """
        |waves {
        |  directory: "/waves"
        |  blockchain {
        |    blockchain-file: ${waves.directory}"/data/blockchain.dat"
        |    state-file: ${waves.directory}"/data/state.dat"
        |    checkpoint-file: ${waves.directory}"/data/checkpoint.dat"
        |    minimum-in-memory-diff-blocks: 201
        |    type: CUSTOM
        |    custom {
        |      address-scheme-character: "C"
        |      functionality {
        |        allow-temporary-negative-until: 1
        |        allow-invalid-payment-transactions-by-timestamp: 2
        |        require-sorted-transactions-after: 3
        |        generation-balance-depth-from-50-to-1000-after-height: 4
        |        minimal-generating-balance-after: 5
        |        allow-transactions-from-future-until: 6
        |        allow-unissued-assets-until: 7
        |        allow-burn-transaction-after: 8
        |        require-payment-unique-id-after: 9
        |        allow-lease-transaction-after: 10
        |        allow-exchange-transaction-after: 11
        |        allow-invalid-reissue-in-same-block-until-timestamp: 12
        |        allow-createalias-transaction-after: 13
        |        allow-multiple-lease-cancel-transaction-until-timestamp: 14
        |        reset-effective-balances-at-height: 15
        |        allow-make-asset-name-unique-transaction-after: 16
        |        allow-leased-balance-transfer-until: 17
        |      }
        |      genesis {
        |        timestamp: 1460678400000
        |        block-timestamp: 1460678400000
        |        signature: "BASE58BLKSGNATURE"
        |        initial-balance: 100000000000000
        |        initial-base-target = 153722867
        |        average-block-delay = 60s
        |        transactions = [
        |          {recipient: "BASE58ADDRESS1", amount: 50000000000001},
        |          {recipient: "BASE58ADDRESS2", amount: 49999999999999}
        |        ]
        |      }
        |    }
        |  }
        |}
      """.stripMargin).resolve()
    val settings = BlockchainSettings.fromConfig(config)

    settings.blockchainFile should be(Some(new File("/waves/data/blockchain.dat")))
    settings.stateFile should be(Some(new File("/waves/data/state.dat")))
    settings.checkpointFile should be(Some(new File("/waves/data/checkpoint.dat")))
    settings.minimumInMemoryDiffSize should be(201)
    settings.addressSchemeCharacter should be('C')
    settings.functionalitySettings.allowTemporaryNegativeUntil should be(1)
    settings.functionalitySettings.allowInvalidPaymentTransactionsByTimestamp should be(2)
    settings.functionalitySettings.requireSortedTransactionsAfter should be(3)
    settings.functionalitySettings.generationBalanceDepthFrom50To1000AfterHeight should be(4)
    settings.functionalitySettings.minimalGeneratingBalanceAfter should be(5)
    settings.functionalitySettings.allowTransactionsFromFutureUntil should be(6)
    settings.functionalitySettings.allowUnissuedAssetsUntil should be(7)
    settings.functionalitySettings.allowBurnTransactionAfter should be(8)
    settings.functionalitySettings.requirePaymentUniqueIdAfter should be(9)
    settings.functionalitySettings.allowLeaseTransactionAfter should be(10)
    settings.functionalitySettings.allowExchangeTransactionAfter should be(11)
    settings.functionalitySettings.allowInvalidReissueInSameBlockUntilTimestamp should be(12)
    settings.functionalitySettings.allowCreatealiasTransactionAfter should be(13)
    settings.functionalitySettings.allowMultipleLeaseCancelTransactionUntilTimestamp should be(14)
    settings.functionalitySettings.resetEffectiveBalancesAtHeight should be(15)
    settings.functionalitySettings.allowMakeAssetNameUniqueTransactionAfter should be(16)
    settings.functionalitySettings.allowLeasedBalanceTransferUntil should be(17)
    settings.genesisSettings.blockTimestamp should be(1460678400000L)
    settings.genesisSettings.timestamp should be(1460678400000L)
    settings.genesisSettings.signature should be(ByteStr.decodeBase58("BASE58BLKSGNATURE").toOption)
    settings.genesisSettings.initialBalance should be(100000000000000L)
    settings.genesisSettings.initialBaseTarget should be(153722867)
    settings.genesisSettings.averageBlockDelay should be(60.seconds)
    settings.genesisSettings.transactions.size should be(2)
    settings.genesisSettings.transactions.head should be(GenesisTransactionSettings("BASE58ADDRESS1", 50000000000001L))
    settings.genesisSettings.transactions.tail.head should be(GenesisTransactionSettings("BASE58ADDRESS2", 49999999999999L))
  }

  it should "read testnet settings" in {
    val config = ConfigFactory.parseString(
      """
        |waves {
        |  directory: "/waves"
        |  blockchain {
        |    blockchain-file: ${waves.directory}"/data/blockchain.dat"
        |    state-file: ${waves.directory}"/data/state.dat"
        |    checkpoint-file: ${waves.directory}"/data/checkpoint.dat"
        |    minimum-in-memory-diff-blocks: 202
        |    type: TESTNET
        |  }
        |}
      """.stripMargin).resolve()
    val settings = BlockchainSettings.fromConfig(config)

    settings.blockchainFile should be(Some(new File("/waves/data/blockchain.dat")))
    settings.stateFile should be(Some(new File("/waves/data/state.dat")))
    settings.checkpointFile should be(Some(new File("/waves/data/checkpoint.dat")))
    settings.minimumInMemoryDiffSize should be(202)
    settings.addressSchemeCharacter should be('T')
    settings.functionalitySettings.allowTemporaryNegativeUntil should be(1477958400000L)
    settings.functionalitySettings.allowInvalidPaymentTransactionsByTimestamp should be(1477958400000L)
    settings.functionalitySettings.requireSortedTransactionsAfter should be(1477958400000L)
    settings.functionalitySettings.generationBalanceDepthFrom50To1000AfterHeight should be(Long.MinValue)
    settings.functionalitySettings.minimalGeneratingBalanceAfter should be(Long.MinValue)
    settings.functionalitySettings.allowTransactionsFromFutureUntil should be(1478100000000L)
    settings.functionalitySettings.allowUnissuedAssetsUntil should be(1479416400000L)
    settings.functionalitySettings.allowBurnTransactionAfter should be(1481110521000L)
    settings.functionalitySettings.requirePaymentUniqueIdAfter should be(1485942685000L)
    settings.functionalitySettings.allowInvalidReissueInSameBlockUntilTimestamp should be(1492560000000L)
    settings.functionalitySettings.allowMultipleLeaseCancelTransactionUntilTimestamp should be(1492560000000L)
    settings.functionalitySettings.allowExchangeTransactionAfter should be(1483228800000L)
    settings.functionalitySettings.resetEffectiveBalancesAtHeight should be(51500)
    settings.functionalitySettings.allowCreatealiasTransactionAfter should be(1493596800000L)
    settings.functionalitySettings.allowMakeAssetNameUniqueTransactionAfter should be(1495238400000L)
    settings.functionalitySettings.allowLeasedBalanceTransferUntil should be(1495238400000L)
    settings.genesisSettings.blockTimestamp should be(1460678400000L)
    settings.genesisSettings.timestamp should be(1478000000000L)
    settings.genesisSettings.signature should be(ByteStr.decodeBase58("5uqnLK3Z9eiot6FyYBfwUnbyid3abicQbAZjz38GQ1Q8XigQMxTK4C1zNkqS1SVw7FqSidbZKxWAKLVoEsp4nNqa").toOption)
    settings.genesisSettings.initialBalance should be(10000000000000000L)
    settings.genesisSettings.transactions.size should be(5)
    settings.genesisSettings.transactions.head should be(GenesisTransactionSettings("3My3KZgFQ3CrVHgz6vGRt8687sH4oAA1qp8", 400000000000000L))
    settings.genesisSettings.transactions.tail.head should be(GenesisTransactionSettings("3NBVqYXrapgJP9atQccdBPAgJPwHDKkh6A8", 200000000000000L))
    settings.genesisSettings.transactions.tail.tail.head should be(GenesisTransactionSettings("3N5GRqzDBhjVXnCn44baHcz2GoZy5qLxtTh", 200000000000000L))
    settings.genesisSettings.transactions.tail.tail.tail.head should be(GenesisTransactionSettings("3NCBMxgdghg4tUhEEffSXy11L6hUi6fcBpd", 200000000000000L))
    settings.genesisSettings.transactions.tail.tail.tail.tail.head should be(GenesisTransactionSettings("3N18z4B8kyyQ96PhN5eyhCAbg4j49CgwZJx", 9000000000000000L))
  }

  it should "read mainnet settings" in {
    val config = ConfigFactory.parseString(
      """
        |waves {
        |  directory: "/waves"
        |  blockchain {
        |    blockchain-file: ${waves.directory}"/data/blockchain.dat"
        |    state-file: ${waves.directory}"/data/state.dat"
        |    checkpoint-file: ${waves.directory}"/data/checkpoint.dat"
        |    minimum-in-memory-diff-blocks: 203
        |    type: MAINNET
        |  }
        |}
      """.stripMargin).resolve()
    val settings = BlockchainSettings.fromConfig(config)

    settings.blockchainFile should be(Some(new File("/waves/data/blockchain.dat")))
    settings.stateFile should be(Some(new File("/waves/data/state.dat")))
    settings.checkpointFile should be(Some(new File("/waves/data/checkpoint.dat")))
    settings.minimumInMemoryDiffSize should be(203)
    settings.addressSchemeCharacter should be('W')
    settings.functionalitySettings.allowTemporaryNegativeUntil should be(1479168000000L)
    settings.functionalitySettings.allowInvalidPaymentTransactionsByTimestamp should be(1479168000000L)
    settings.functionalitySettings.requireSortedTransactionsAfter should be(1479168000000L)
    settings.functionalitySettings.generationBalanceDepthFrom50To1000AfterHeight should be(232000L)
    settings.functionalitySettings.minimalGeneratingBalanceAfter should be(1479168000000L)
    settings.functionalitySettings.allowTransactionsFromFutureUntil should be(1479168000000L)
    settings.functionalitySettings.allowUnissuedAssetsUntil should be(1479416400000L)
    settings.functionalitySettings.allowBurnTransactionAfter should be(1491192000000L)
    settings.functionalitySettings.allowInvalidReissueInSameBlockUntilTimestamp should be(1492768800000L)
    settings.functionalitySettings.allowMultipleLeaseCancelTransactionUntilTimestamp should be(1492768800000L)
    settings.functionalitySettings.resetEffectiveBalancesAtHeight should be(462000)
    settings.functionalitySettings.requirePaymentUniqueIdAfter should be(1491192000000L)
    settings.functionalitySettings.allowExchangeTransactionAfter should be(1491192000000L)
    settings.functionalitySettings.allowMakeAssetNameUniqueTransactionAfter should be(Long.MaxValue)
    settings.functionalitySettings.allowLeasedBalanceTransferUntil should be(Long.MaxValue)
    settings.genesisSettings.blockTimestamp should be(1460678400000L)
    settings.genesisSettings.timestamp should be(1465742577614L)
    settings.genesisSettings.signature should be(ByteStr.decodeBase58("FSH8eAAzZNqnG8xgTZtz5xuLqXySsXgAjmFEC25hXMbEufiGjqWPnGCZFt6gLiVLJny16ipxRNAkkzjjhqTjBE2").toOption)
    settings.genesisSettings.initialBalance should be(10000000000000000L)
    settings.genesisSettings.transactions.size should be(6)
    settings.genesisSettings.transactions.head should be(GenesisTransactionSettings("3PAWwWa6GbwcJaFzwqXQN5KQm7H96Y7SHTQ", 9999999500000000L))
    settings.genesisSettings.transactions.tail.head should be(GenesisTransactionSettings("3P8JdJGYc7vaLu4UXUZc1iRLdzrkGtdCyJM", 100000000L))
    settings.genesisSettings.transactions.tail.tail.head should be(GenesisTransactionSettings("3PAGPDPqnGkyhcihyjMHe9v36Y4hkAh9yDy", 100000000L))
    settings.genesisSettings.transactions.tail.tail.tail.head should be(GenesisTransactionSettings("3P9o3ZYwtHkaU1KxsKkFjJqJKS3dLHLC9oF", 100000000L))
    settings.genesisSettings.transactions.tail.tail.tail.tail.head should be(GenesisTransactionSettings("3PJaDyprvekvPXPuAtxrapacuDJopgJRaU3", 100000000L))
    settings.genesisSettings.transactions.tail.tail.tail.tail.tail.head should be(GenesisTransactionSettings("3PBWXDFUc86N2EQxKJmW8eFco65xTyMZx6J", 100000000L))
  }
}
