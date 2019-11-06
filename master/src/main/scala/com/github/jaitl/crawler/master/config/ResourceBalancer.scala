package com.github.jaitl.crawler.master.config

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.master.config.ProxyRequestController.RequestProxy
import com.github.jaitl.crawler.master.config.TorRequestController.RequestTor
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestResource

class ResourceBalancer(
  proxiesBalancer: ActorRef,
  torsBalancer: ActorRef
) extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case RequestResource(requestId, projectConfiguration) if projectConfiguration.workerResource.nonEmpty =>
      log.debug(s"RequestConfiguration, requestId: $requestId, config: $projectConfiguration")
      projectConfiguration.workerResource match {
        case "Tor" => proxiesBalancer ! RequestProxy(requestId, projectConfiguration, sender())
        case "Proxy" => torsBalancer ! RequestTor(requestId, projectConfiguration, sender())
      }
  }
}
object ResourceBalancer {
  def props(proxiesBalancer: ActorRef, torsBalancer: ActorRef): Props =
    Props(new ResourceBalancer(proxiesBalancer, torsBalancer))

  def name(): String = "resourceBalancer"

  case object Stop

}
