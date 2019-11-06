package com.github.jaitl.crawler.master.config

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestConfiguration

class ConfigurationBalancer(
  configurationReqCtrl: ActorRef
) extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case RequestConfiguration(requestId, taskType) if taskType.nonEmpty =>
      log.debug(s"RequestConfiguration, requestId: $requestId, type: $taskType")
      configurationReqCtrl ! ConfigurationRequestController.RequestConfiguration(requestId, taskType, sender())
  }
}
object ConfigurationBalancer {
  def props(configurationReqCtrl: ActorRef): Props =
    Props(new ConfigurationBalancer(configurationReqCtrl))

  def name(): String = "configurationBalancer"

  case object Stop

}
