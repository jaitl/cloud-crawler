package com.github.jaitl.crawler.master.queue

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import scala.concurrent.Future

abstract class ActorTestSuite
  extends TestKit(ActorSystem("CloudCrawlerTest"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with MockFactory {

  protected val futureSuccess: Future[Unit] = Future.successful(Unit)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}
