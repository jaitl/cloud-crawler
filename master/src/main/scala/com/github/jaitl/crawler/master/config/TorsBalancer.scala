package com.github.jaitl.crawler.master.config

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestTor

class TorsBalancer(
  torsRecCtrl: ActorRef
) extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case RequestTor(requestId, taskType) if taskType.nonEmpty =>
      log.debug(s"RequestProxy, requestId: $requestId, type: $taskType")
      torsRecCtrl ! TorRequestController.RequestTor(requestId, taskType, sender())
  }
}

object TorsBalancer {
  def props(torsRecCtrl: ActorRef): Props =
    Props(new TorsBalancer(torsRecCtrl))

  def name(): String = "torsBalancer"

  case object Stop

}
