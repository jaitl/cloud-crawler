package com.github.jaitl.crawler.worker.executor.resource

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.RequestResource

import scala.concurrent.ExecutionContext

private class ProxyResourceController extends Actor with ActorLogging {
  private implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case RequestResource(requestId) =>

  }
}

private object ProxyResourceController {
  def props: Props = Props(new ProxyResourceController)
  def name: String = s"proxyResourceController"
}
