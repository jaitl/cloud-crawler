package com.github.jaitl.crawler.base.worker.executor.resource

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.RequestResource

private class TorResourceController extends Actor with ActorLogging{
  override def receive: Receive = {
    case RequestResource(requestId, taskType) =>

  }
}

private object TorResourceController {
  def props: Props = Props(new TorResourceController)
  def name: String = s"torResourceController"
}
