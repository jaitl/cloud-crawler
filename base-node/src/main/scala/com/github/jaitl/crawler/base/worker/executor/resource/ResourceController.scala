package com.github.jaitl.crawler.base.worker.executor.resource

import java.util.UUID

import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import com.github.jaitl.crawler.base.worker.creator.OneArgumentActorCreator
import com.github.jaitl.crawler.base.worker.executor.resource.TorResourceController.TorConfig
import com.github.jaitl.crawler.base.worker.http.HttpRequestExecutor
import com.github.jaitl.crawler.base.worker.pipeline.Proxy
import com.github.jaitl.crawler.base.worker.pipeline.ResourceType
import com.github.jaitl.crawler.base.worker.pipeline.Tor

private[base] object ResourceController {
  case class RequestResource(requestId: UUID)

  case class SuccessRequestResource(requestId: UUID, requestExecutor: HttpRequestExecutor)

  case class NoResourcesAvailable(requestId: UUID)
  case class NoFreeResource(requestId: UUID)

  case class ReturnSuccessResource(requestId: UUID, requestExecutor: HttpRequestExecutor)
  case class ReturnFailedResource(requestId: UUID, requestExecutor: HttpRequestExecutor, t: Throwable)
}

case class ResourceControllerConfig(maxFailCount: Int)

private[base] class ResourceControllerCreator(
  ctrlConfig: ResourceControllerConfig
) extends OneArgumentActorCreator[ResourceType] {
  override def create(factory: ActorRefFactory, firstArg: ResourceType): ActorRef = {
    firstArg match {
      case Proxy(_, _) =>
        factory.actorOf(
          props = ProxyResourceController.props.withDispatcher("worker.blocking-io-dispatcher"),
          name = ProxyResourceController.name
        )
      case Tor(host, port, limit, timeout) =>
        val config = TorConfig(host, port, limit, ctrlConfig.maxFailCount, timeout)
        factory.actorOf(
          props = TorResourceController.props(config).withDispatcher("worker.blocking-io-dispatcher"),
          name = TorResourceController.name
        )
      case a: Any => throw new NoSuchResourceTypeException(s"unknown type: $a")
    }
  }
}

class NoSuchResourceTypeException(message: String) extends Exception(message)
