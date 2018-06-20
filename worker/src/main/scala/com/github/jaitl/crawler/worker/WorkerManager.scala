package com.github.jaitl.crawler.worker

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.models.task.TasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.EmptyTaskTypeList
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoTasks
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestTasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.TaskTypeWithBatchSize
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchController
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.Scheduler

private[worker] class WorkerManager(
  queueTaskBalancer: ActorRef,
  pipelines: Map[String, Pipeline[_]],
  config: WorkerConfig,
  tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline[_]],
  batchRequestScheduler: Scheduler
) extends Actor {
  private val taskTypes = pipelines.values.map(pipe => TaskTypeWithBatchSize(pipe.taskType, pipe.batchSize)).toSeq
  private val cancellable = batchRequestScheduler.schedule(config.executeInterval, self, RequestBatch)

  override def receive: Receive = {
    case RequestBatch =>
      if (context.children.size < config.parallelBatches) {
        queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), taskTypes)
      }

    case SuccessTasksBatchRequest(requestId, taskType, tasksBatch) =>
      val tasksBatchController = tasksBatchControllerCreator.create(this.context, tasksBatch, pipelines(taskType))
      tasksBatchController ! TasksBatchController.ExecuteTask

    case FailureTasksBatchRequest(requestId, taskType, throwable) =>

    case NoTasks(requestId, taskType) =>

    case EmptyTaskTypeList =>
  }
}

private[worker] object WorkerManager {
  def props(
    queueTaskBalancer: ActorRef,
    pipelines: Map[String, Pipeline[_]],
    config: WorkerConfig,
    tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline[_]],
    batchRequestScheduler: Scheduler
  ): Props =
    Props(new WorkerManager(queueTaskBalancer, pipelines, config, tasksBatchControllerCreator, batchRequestScheduler))

  def name(): String = "workerManager"
}
