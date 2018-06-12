package com.github.jaitl.crawler.base.worker.executor.resource

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.NoFreeResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.RequestResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.ReturnFailedResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.ReturnSuccessResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.crawler.base.worker.executor.resource.TorResourceController.TorConfig
import com.github.jaitl.crawler.base.worker.timeout.RandomTimeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuiteLike
import org.scalatest.Matchers

import scala.concurrent.duration._

class TorResourceControllerTest extends TestKit(ActorSystem("MySpec")) with FunSuiteLike
  with Matchers with ImplicitSender with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  test("new executor") {
    val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(1.millis, 1.millis))

    val torController = TestActorRef[TorResourceController](TorResourceController.props(torConfig))

    val id = UUID.randomUUID()
    torController ! RequestResource(id)

    val msg = expectMsgType[SuccessRequestResource]
    val executor = msg.requestExecutor

    msg.requestId shouldBe id
    torController.underlyingActor.executors should have size 1
    torController.underlyingActor.executors(executor.getExecutorId()).isUsed shouldBe true
    torController.underlyingActor.executors(executor.getExecutorId()).awaitTo.isDefined shouldBe false
  }

  test("return executor") {
    val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(0.millis, 0.millis))

    val torController = TestActorRef[TorResourceController](TorResourceController.props(torConfig))

    torController ! RequestResource(UUID.randomUUID())

    val msg = expectMsgType[SuccessRequestResource]
    val executor = msg.requestExecutor

    torController.underlyingActor.executors should have size 1
    torController.underlyingActor.executors(executor.getExecutorId()).isUsed shouldBe true
    torController.underlyingActor.executors(executor.getExecutorId()).awaitTo.isDefined shouldBe false

    torController ! ReturnSuccessResource(UUID.randomUUID(), msg.requestExecutor)

    torController.underlyingActor.executors should have size 1
    torController.underlyingActor.executors(executor.getExecutorId()).isUsed shouldBe false
    torController.underlyingActor.executors(executor.getExecutorId()).awaitTo.isDefined shouldBe true
  }

  test("executor from cache") {
    val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(0.millis, 0.millis))

    val torController = TestActorRef[TorResourceController](TorResourceController.props(torConfig))

    torController ! RequestResource(UUID.randomUUID())
    var msg = expectMsgType[SuccessRequestResource]

    torController ! ReturnSuccessResource(UUID.randomUUID(), msg.requestExecutor)

    Thread.sleep(10)

    torController ! RequestResource(UUID.randomUUID())
    msg = expectMsgType[SuccessRequestResource]
  }

  test("NoFreeResource") {
    val torConfig = TorConfig("0", 0, 1, 10, RandomTimeout(1.minute, 0.millis))
    val torController = system.actorOf(TorResourceController.props(torConfig))

    torController ! RequestResource(UUID.randomUUID())
    expectMsgType[SuccessRequestResource]

    torController ! RequestResource(UUID.randomUUID())
    expectMsgType[NoFreeResource]
  }

  test("NoResourcesAvailable") {
    val torConfig = TorConfig("0", 0, 1, 2, RandomTimeout(0.minute, 0.millis))
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
