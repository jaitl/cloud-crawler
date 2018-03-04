package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.cloud.base.WorkerManager.RequestBatch
import com.github.jaitl.cloud.base.config.WorkerConfig
import com.github.jaitl.cloud.base.executor.TasksBatchController
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.request.FailureTasksBatchRequest
import com.github.jaitl.cloud.common.models.request.NoTasks
import com.github.jaitl.cloud.common.models.request.RequestTasksBatch
import com.github.jaitl.cloud.common.models.request.SuccessTasksBatchRequest

private class WorkerManager(
  queueTaskBalancer: ActorRef,
  crawlExecutorRouter: ActorRef,
  pipelines: Seq[Pipeline],
  config: WorkerConfig
) extends Actor {
  override def receive: Receive = {
    case RequestBatch =>
      if (context.children.size < config.parallelBatches) {
        queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), Seq("type1", "type2"), 2)
      } else {
        scheduleTimeout()
      }

    case SuccessTasksBatchRequest(requestId, taskType, tasksBatch) =>
      self ! RequestBatch

      context.actorOf(
        TasksBatchController.props(tasksBatch, crawlExecutorRouter, pipelines.head),
        TasksBatchController.name(tasksBatch.id)
      )

    case FailureTasksBatchRequest(requestId, taskType, throwable) =>
      scheduleTimeout()

    case NoTasks(requestId, taskType) =>
      scheduleTimeout()
  }

  private def scheduleTimeout(): Unit = ???
}

private object WorkerManager {

  case object RequestBatch

  def props(
    queueTaskBalancer: ActorRef,
    crawlExecutorRouter: ActorRef,
    pipelines: Seq[Pipeline],
    config: WorkerConfig
  ): Props =
    Props(new WorkerManager(queueTaskBalancer, crawlExecutorRouter, pipelines, config))

  def name(): String = "workerManager"
}
