package com.github.jaitl.crawler.master

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import scala.concurrent.Future

abstract class ActorTestSuite
  extends TestKit(ActorSystem("CloudCrawlerTest", ConfigFactory.parseString("""
  akka.loggers = ["akka.testkit.TestEventListener"]
  """)))
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
