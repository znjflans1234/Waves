package com.wavesplatform.mining

import java.util.concurrent.{Executors, TimeUnit}

import com.wavesplatform.settings.BlockchainSettings
import com.wavesplatform.state2.reader.StateReader
import scorex.account.PrivateKeyAccount
import scorex.block.Block
import scorex.consensus.nxt.NxtLikeConsensusBlockData
import scorex.transaction.PoSCalc._
import scorex.transaction.UnconfirmedTransactionsStorage.packUnconfirmed
import scorex.transaction.{History, PoSCalc, UnconfirmedTransactionsStorage}
import scorex.utils.{ScorexLogging, Time}

import scala.util.control.NonFatal

class Miner(
    history: History,
    state: StateReader,
    utx: UnconfirmedTransactionsStorage,
    privateKeyAccounts: => Seq[PrivateKeyAccount],
    time: Time,
    bcs: BlockchainSettings,
    blockHandler: Block => Unit) extends ScorexLogging {
  import Miner._

  private val minerPool = Executors.newScheduledThreadPool(4)

  def lastBlockChanged(parentHeight: Int, parent: Block): Unit = {
    val greatGrandParent = history.blockAt(parentHeight - 3)
    for (account <- privateKeyAccounts; ts <- PoSCalc.nextBlockGenerationTime(parentHeight, state, bcs.functionalitySettings, parent, account)) {
      minerPool.schedule((() => {
        try {
          val balance = generatingBalance(state, bcs.functionalitySettings)(account, parentHeight)

          require(balance < MinimalEffectiveBalanceForGenerator,
            s"Effective balance $balance is less that minimal ($MinimalEffectiveBalanceForGenerator)")

          val lastBlockKernelData = parent.consensusData
          val currentTime = time.correctedTime()

          val h = calcHit(lastBlockKernelData, account)
          val t = calcTarget(parent, currentTime, balance)

          require(h < t, s"Hit $h was NOT less than target $t")

          val eta = (currentTime - parent.timestamp) / 1000

          log.debug(s"hit=$h, target=$t, ${if (h < t) "" else "NOT"} generating, eta=$eta, account=$account, " +
            s"balance=$balance, lastBlockId=${parent.encodedId}, height=$parentHeight, lastTarget=${lastBlockKernelData.baseTarget}")

          val avgBlockDelay = bcs.genesisSettings.averageBlockDelay
          val btg = calcBaseTarget(avgBlockDelay, parentHeight, parent, greatGrandParent, currentTime)
          val gs = calcGeneratorSignature(lastBlockKernelData, account)
          val consensusData = NxtLikeConsensusBlockData(btg, gs)

          val unconfirmed = packUnconfirmed(state, bcs.functionalitySettings, utx, time, parentHeight)
          log.debug(s"Building block with ${unconfirmed.size} transactions approx. $eta seconds after previous block")

          blockHandler(Block.buildAndSign(Version,
            currentTime,
            parent.uniqueId,
            consensusData,
            unconfirmed,
            account))
        } catch {
          case NonFatal(e) => log.warn("Error generating block", e)
        }
      }): Runnable, ts - time.correctedTime(), TimeUnit.MILLISECONDS)
    }
  }

  def shutdown(): Unit = {
    minerPool.shutdownNow()
  }
}

object Miner {
  val Version: Byte = 2
}