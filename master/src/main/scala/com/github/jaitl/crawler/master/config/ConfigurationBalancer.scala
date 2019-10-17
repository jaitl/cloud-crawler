package com.github.jaitl.crawler.master.config

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import com.github.jaitl.crawler.models.worker.WorkerManager.EmptyTaskTypeList
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestConfiguration
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestTasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.ReturnTasks
import com.github.jaitl.crawler.models.worker.WorkerManager.TasksBatchProcessResult

import scala.util.Random

class ConfigurationBalancer(
  configurationReqCtrl: ActorRef,
  configurationResCtrl: ActorRef
) extends Actor
    with ActorLogging {

  // scalastyle:off method.length
  override def receive: Receive = {
    case RequestConfiguration(requestId, taskType) if taskType.nonEmpty =>
  }
}
