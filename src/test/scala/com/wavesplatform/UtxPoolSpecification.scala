package com.wavesplatform

import com.wavesplatform.history.BlockStorageImpl
import com.wavesplatform.network.RawBytes
import com.wavesplatform.settings.{BlockchainSettings, FeeSettings, FeesSettings, FunctionalitySettings, UtxSettings}
import com.wavesplatform.state2.diffs.produce
import io.netty.channel.group.{ChannelGroup, ChannelMatcher, ChannelMatchers}
import org.scalacheck.Gen._
import org.scalacheck.{Gen, Shrink}
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FreeSpec, Matchers}
import scorex.account.{Account, PrivateKeyAccount}
import scorex.block.Block
import scorex.transaction.assets.TransferTransaction
import scorex.transaction.{FeeCalculator, Transaction}
import scorex.utils.Time

import scala.concurrent.duration._

class UtxPoolSpecification extends FreeSpec
    with Matchers
    with MockFactory
    with PropertyChecks
    with GeneratorDrivenPropertyChecks
    with TransactionGen {

  private implicit def noShrink[A]: Shrink[A] = Shrink(_ => Stream.empty)

  private val group = mock[ChannelGroup]
  private val calculator = new FeeCalculator(FeesSettings(Map(
    1 -> List(FeeSettings("", 0)),
    4 -> List(FeeSettings("", 0))
  )))

  private def mkState(senderAccount: Account, senderBalance: Long) = {
    val genesisSettings = TestHelpers.genesisSettings(Map(senderAccount -> senderBalance))
    val (_, _, state, bcu) =
      BlockStorageImpl(BlockchainSettings(None, None, None, 'T', 5, FunctionalitySettings.TESTNET, genesisSettings)).get

    bcu.processBlock(Block.genesis(genesisSettings).right.get)

    state
  }

  private def transfer(sender: PrivateKeyAccount, maxAmount: Long, time: Time) = (for {
    amount <- chooseNum(1, (maxAmount * 0.9).toLong)
    recipient <- accountGen
    fee <- chooseNum(1, (maxAmount * 0.1).toLong)
  } yield TransferTransaction.create(None, sender, recipient, amount, time.getTimestamp(), None, fee, Array.empty[Byte]).right.get)
    .label("transferTransaction")

  private def expectBroadcast(tx: Transaction): Unit =
    (group.writeAndFlush(_: Any, _: ChannelMatcher)).expects(RawBytes(25, tx.bytes), ChannelMatchers.all()).once()

  private val stateGen = for {
    sender <- accountGen.label("sender")
    senderBalance <- positiveLongGen.label("senderBalance")
  } yield (sender, senderBalance, mkState(sender, senderBalance))

  private def utxTest(utxSettings: UtxSettings = UtxSettings(20, 5.seconds), txCount: Int = 10)
                     (f: (Seq[TransferTransaction], UtxPool, TestTime) => Unit): Unit = forAll(
    stateGen,
    chooseNum(2, txCount).label("txCount")) { case ((sender, senderBalance, state), count) =>
      val time = new TestTime().reset()

      forAll(listOfN(count, transfer(sender, senderBalance / 2, time))) { txs =>
        val utx = new UtxPool(group, time, state, calculator, FunctionalitySettings.TESTNET, utxSettings)
        f(txs, utx, time)
      }
    }

  private val dualTxGen: Gen[(UtxPool, TestTime, Seq[Transaction], FiniteDuration, Seq[Transaction])] =
    for {
      (sender, senderBalance, state) <- stateGen
      ts = System.currentTimeMillis()
      count1 <- chooseNum(5, 10)
      tx1 <- listOfN(count1, transfer(sender, senderBalance / 2, new TestTime(ts)))
      offset <- chooseNum(5000L, 10000L)
      tx2 <- listOfN(count1, transfer(sender, senderBalance / 2, new TestTime(ts + offset + 1000)))
    } yield {
      val time = new TestTime(System.currentTimeMillis())
      val utx = new UtxPool(group, time, state, calculator, FunctionalitySettings.TESTNET, UtxSettings(10, offset.millis))
      (utx, time, tx1, (offset + 1000).millis, tx2)
    }

  "UTX Pool" - {
    "does not add new transactions when full" in utxTest(UtxSettings(1, 5.seconds)) { (txs, utx, _) =>
      expectBroadcast(txs.head)
      utx.putIfNew(txs.head) shouldBe 'right
      all(txs.tail.map(t => utx.putIfNew(t))) shouldBe 'left
    }

    "does not add the same transaction twice" in utxTest() { (txs, utx, _) =>
      expectBroadcast(txs.head)
      utx.putIfNew(txs.head) shouldBe 'right
      utx.putIfNew(txs.head) should produce(s"Transaction ${txs.head.id} already in the pool")
    }

    "evicts expired transactions when new ones are added" in forAll(dualTxGen) { case (utx, time, txs1, offset, txs2) =>
      all(txs1.map { t =>
        expectBroadcast(t)
        utx.putIfNew(t)
      }) shouldBe 'right
      utx.all().size shouldEqual txs1.size

      time.advance(offset)

      all(txs2.map { t =>
        expectBroadcast(t)
        utx.putIfNew(t)
      }) shouldBe 'right
      utx.all().size shouldEqual txs2.size
    }

    "evicts expired transactions when packUnconfirmed is called" in forAll(dualTxGen) { case (utx, time, txs, offset, _) =>
      all(txs.map { t =>
        expectBroadcast(t)
        utx.putIfNew(t)
      }) shouldBe 'right
      utx.all().size shouldEqual txs.size

      time.advance(offset)

      utx.packUnconfirmed() shouldBe 'empty
      utx.all() shouldBe 'empty
    }
  }
}
