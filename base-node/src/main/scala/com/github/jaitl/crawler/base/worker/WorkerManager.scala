package com.github.jaitl.crawler.base.worker

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.base.common.request.FailureTasksBatchRequest
import com.github.jaitl.crawler.base.common.request.NoTasks
import com.github.jaitl.crawler.base.common.request.RequestTasksBatch
import com.github.jaitl.crawler.base.common.request.SuccessTasksBatchRequest
import com.github.jaitl.crawler.base.common.request.TaskTypeWithBatchSize
import com.github.jaitl.crawler.base.common.task.TasksBatch
import com.github.jaitl.crawler.base.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.base.worker.config.WorkerConfig
import com.github.jaitl.crawler.base.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline

private class WorkerManager(
  queueTaskBalancer: ActorRef,
  pipelines: Map[String, Pipeline],
  config: WorkerConfig,
  tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline]
) extends Actor {
  private val taskTypes = pipelines.values.map(pipe => TaskTypeWithBatchSize(pipe.taskType, pipe.batchSize)).toSeq

  override def receive: Receive = {

    case RequestBatch =>
      if (context.children.size < config.parallelBatches) {
        queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), taskTypes)
      } else {
        scheduleTimeout()
      }

    case SuccessTasksBatchRequest(requestId, taskType, tasksBatch) =>
      self ! RequestBatch

      val tasksBatchController = tasksBatchControllerCreator.create(this.context, tasksBatch, pipelines(taskType))
      tasksBatchController ! TasksBatchController.ExecuteTask

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
    pipelines: Map[String, Pipeline],
    config: WorkerConfig,
    tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline]
  ): Props =
    Props(new WorkerManager(queueTaskBalancer, pipelines, config, tasksBatchControllerCreator))

  def name(): String = "workerManager"
}
