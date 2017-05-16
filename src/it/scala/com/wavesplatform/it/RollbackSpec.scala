package com.wavesplatform.it

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.collection.mutable
import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.traverse
import scala.concurrent.duration._

class RollbackSpec(override val allNodes: Seq[Node]) extends FreeSpec with ScalaFutures with IntegrationPatience
  with Matchers with TransferSending {
  "Apply 100 transfer transactions and rollback twice" in {
    result(for {
      b <- traverse(allNodes)(balanceForNode).map(mutable.AnyRefMap[String, Long](_: _*))
      startHeight <- Future.traverse(allNodes)(_.height).map(_.max)
      startInfo <- traverse(allNodes)(_.waitForDebugInfoAt(startHeight + 5)).map(infos => {
        all(infos) shouldEqual infos.head
        infos.head
      })
      requsts = generateRequests(1000, b)
      _ <- processRequests(requsts)
      afterFirstTryInfo <- traverse(allNodes)(_.waitForDebugInfoAt(startHeight + 20)).map(infos => {
        all(infos) shouldEqual infos.head
        infos.head
      })
      _ <- traverse(allNodes)(_.rollback(startHeight))
      secondStartInfo <- traverse(allNodes)(_.waitForDebugInfoAt(startHeight + 5)).map(infos => {
        all(infos) shouldEqual infos.head
        infos.head
      })
      _ <- processRequests(requsts)
      afterSecondTryInfo <- traverse(allNodes)(_.waitForDebugInfoAt(startHeight + 20)).map(infos => {
        all(infos) shouldEqual infos.head
        infos.head
      })
    } yield {
      startInfo shouldBe secondStartInfo
      afterFirstTryInfo shouldBe afterSecondTryInfo
    }, 5.minutes)
  }
}
