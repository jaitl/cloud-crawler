package com.github.jaitl.crawler.master.queue.provider

import java.time.Instant

import com.github.jaitl.crawler.master.client.task.Task

import scala.concurrent.Future

trait QueueTaskProvider {
  def pullBatch(taskType: String, size: Int): Future[Seq[Task]]

  def pullAndUpdateStatus(taskType: String, size: Int, taskStatus: String): Future[Seq[Task]]

  def pushTasks(taskData: Seq[Task]): Future[Unit]

  def updateTasksStatus(ids: Seq[String], taskStatus: String): Future[Unit]

  def updateTasksStatusFromTo(time: Instant, fromStatus: String, toStatus: String): Future[Long]

  def updateTasksStatusAndIncAttempt(ids: Seq[String], taskStatus: String): Future[Unit]

  def dropTasks(ids: Seq[String]): Future[Unit]

  def getByIds(ids: Seq[String]): Future[Seq[Task]]
}
