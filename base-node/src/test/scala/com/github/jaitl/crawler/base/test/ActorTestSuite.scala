package com.github.jaitl.crawler.base.test

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuiteLike
import org.scalatest.Matchers

abstract class ActorTestSuite
  extends TestKit(ActorSystem("CloudCrawlerTest"))
  with ImplicitSender
  with FunSuiteLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}
