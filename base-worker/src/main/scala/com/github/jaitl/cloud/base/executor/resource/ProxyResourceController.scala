package com.github.jaitl.cloud.base.executor.resource

import akka.actor.Actor
import akka.actor.ActorLogging
import com.github.jaitl.cloud.base.executor.resource.ResourceController.RequestResource

private class ProxyResourceController extends Actor with ActorLogging{
  override def receive: Receive = {
    case RequestResource(requestId, taskType) =>

  }
}

