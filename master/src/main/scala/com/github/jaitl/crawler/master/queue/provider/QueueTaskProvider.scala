package com.github.jaitl.crawler.master.queue.provider

import com.github.jaitl.crawler.models.task.Task

import scala.concurrent.Future

trait QueueTaskProvider {
  def pullBatch(taskType: String, size: Int): Future[Seq[Task]]

  def pushTasks(taskType: String, taskData: Seq[String]): Future[Unit]

  def updateTasksStatus(ids: Seq[String], taskStatus: String): Future[Unit]

  def dropTasks(ids: Seq[String]): Future[Unit]
}
