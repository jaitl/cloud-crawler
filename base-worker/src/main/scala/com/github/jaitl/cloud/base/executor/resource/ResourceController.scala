package com.github.jaitl.cloud.base.executor.resource

import java.util.UUID

import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import com.github.jaitl.cloud.base.creator.OneArgumentActorCreator
import com.github.jaitl.cloud.base.http.HttpRequestExecutor
import com.github.jaitl.cloud.base.pipeline.Proxy
import com.github.jaitl.cloud.base.pipeline.ResourceType
import com.github.jaitl.cloud.base.pipeline.Tor

private[base] object ResourceController {

  case class RequestResource(requestId: UUID, taskType: String)

  case class SuccessRequestResource(requestId: UUID, requestExecutor: HttpRequestExecutor)

  case class NoResourcesAvailable(requestId: UUID)

  case class NoFreeResource(requestId: UUID)


  case class ReturnSuccessResource(requestId: UUID, taskType: String, requestExecutor: HttpRequestExecutor)
  case class ReturnFailedResource(requestId: UUID, taskType: String, requestExecutor: HttpRequestExecutor, t: Throwable)
}

private[base] class ResourceControllerCreator extends OneArgumentActorCreator[ResourceType] {
  override def create(factory: ActorRefFactory, firstArg: ResourceType): ActorRef = {
    firstArg match {
      case Proxy(_) => factory.actorOf(ProxyResourceController.props, ProxyResourceController.name)
      case Tor(_, _, _) => factory.actorOf(TorResourceController.props, TorResourceController.name)
      case a: Any => throw new NoSuchResourceType(s"unknown type: $a")
    }
  }
}

class NoSuchResourceType(message: String) extends Exception(message)
