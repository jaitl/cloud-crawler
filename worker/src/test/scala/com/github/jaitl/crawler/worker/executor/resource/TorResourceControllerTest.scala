package com.github.jaitl.crawler.worker.executor.resource

import java.util.UUID

import akka.testkit.TestActorRef
import com.github.jaitl.crawler.worker.ActorTestSuite
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.NoFreeResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.RequestResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnFailedResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnSuccessResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.crawler.worker.executor.resource.TorResourceController.TorConfig
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import org.scalatest.Ignore

import scala.concurrent.duration._

@Ignore
class TorResourceControllerTest extends ActorTestSuite {
  "TorResourceController" should {

    "new executor" in {
      val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(1.millis, 1.millis), 0, "")

      val torController = TestActorRef[TorResourceController](TorResourceController.props(torConfig))

      val id = UUID.randomUUID()
      torController ! RequestResource(id)

      val msg = expectMsgType[SuccessRequestResource]
      val executor = msg.requestExecutor

      msg.requestId shouldBe id
      (torController.underlyingActor.executors should have).size(1)
      torController.underlyingActor.executors(executor.getExecutorId()).isUsed shouldBe true
      torController.underlyingActor.executors(executor.getExecutorId()).awaitTo.isDefined shouldBe false
    }

    "return executor" in {
      val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(0.millis, 0.millis), 0, "")

      val torController = TestActorRef[TorResourceController](TorResourceController.props(torConfig))

      torController ! RequestResource(UUID.randomUUID())

      val msg = expectMsgType[SuccessRequestResource]
      val executor = msg.requestExecutor

      (torController.underlyingActor.executors should have).size(1)
      torController.underlyingActor.executors(executor.getExecutorId()).isUsed shouldBe true
      torController.underlyingActor.executors(executor.getExecutorId()).awaitTo.isDefined shouldBe false

      torController ! ReturnSuccessResource(UUID.randomUUID(), msg.requestExecutor)

      (torController.underlyingActor.executors should have).size(1)
      torController.underlyingActor.executors(executor.getExecutorId()).isUsed shouldBe false
      torController.underlyingActor.executors(executor.getExecutorId()).awaitTo.isDefined shouldBe true
    }

    "executor from cache" in {
      val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(0.millis, 0.millis), 0, "")

      val torController = TestActorRef[TorResourceController](TorResourceController.props(torConfig))

      torController ! RequestResource(UUID.randomUUID())
      var msg = expectMsgType[SuccessRequestResource]

      torController ! ReturnSuccessResource(UUID.randomUUID(), msg.requestExecutor)

      Thread.sleep(10)

      torController ! RequestResource(UUID.randomUUID())
      msg = expectMsgType[SuccessRequestResource]
    }

    "NoFreeResource" in {
      val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(1.minute, 0.millis), 0, "")
      val torController = system.actorOf(TorResourceController.props(torConfig))

      torController ! RequestResource(UUID.randomUUID())
      expectMsgType[SuccessRequestResource]

      torController ! RequestResource(UUID.randomUUID())
      expectMsgType[NoFreeResource]
    }

    "NoResourcesAvailable" in {
      val torConfig = TorConfig("0", 0, 1, 2, RandomTimeout(0.minute, 0.millis), 0, "")
      val torController = system.actorOf(TorResourceController.props(torConfig))

      torController ! RequestResource(UUID.randomUUID())
      var msg = expectMsgType[SuccessRequestResource]

      torController ! ReturnFailedResource(UUID.randomUUID(), msg.requestExecutor, new Exception())

      Thread.sleep(10)

      torController ! RequestResource(UUID.randomUUID())
      msg = expectMsgType[SuccessRequestResource]

      torController ! ReturnFailedResource(UUID.randomUUID(), msg.requestExecutor, new Exception())

      Thread.sleep(10)

      torController ! RequestResource(UUID.randomUUID())
      expectMsgType[NoResourcesAvailable]
    }
  }
}
