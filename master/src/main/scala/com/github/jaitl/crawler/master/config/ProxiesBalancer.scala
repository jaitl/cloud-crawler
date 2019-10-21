package com.github.jaitl.crawler.master.config

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestProxy

class ProxiesBalancer(
  proxiesRecCtrl: ActorRef
) extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case RequestProxy(requestId, taskType) if taskType.nonEmpty =>
      log.debug(s"RequestProxy, requestId: $requestId, type: $taskType")
      proxiesRecCtrl ! ProxyRequestController.RequestProxy(requestId, taskType, sender())
  }
}

object ProxiesBalancer {
  def props(proxiesRecCtrl: ActorRef): Props =
    Props(new ProxiesBalancer(proxiesRecCtrl))

  def name(): String = "proxiesBalancer"

  case object Stop

}