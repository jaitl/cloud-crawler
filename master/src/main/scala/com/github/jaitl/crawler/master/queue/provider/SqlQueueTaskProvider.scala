package com.github.jaitl.crawler.master.queue.provider

import java.time.Instant

import com.github.jaitl.crawler.master.client.task.Task
import scalikejdbc.ConnectionPool
import scalikejdbc._

import scala.concurrent.Future

class SqlQueueTaskProvider(
  driverName: String,
  connectionUrl: String,
  user: String,
  password: String
) extends QueueTaskProvider {
  Class.forName(driverName)
  ConnectionPool.singleton(connectionUrl, user, password)

  override def pullBatch(taskType: String, size: Int): Future[Seq[Task]] =
    Future.successful(DB.localTx { implicit session =>
      sql"select * from projects_url pu, projects p where p.type = ${taskType} and p.id = pu.project_id and pu.status = ${TaskStatus.taskWait} limit ${size}"
        .map(
          rs =>
            Task(
              id = rs.get("id"),
              taskType = rs.get("type"),
              taskData = rs.get("url")
          ))
        .list()
        .apply()
    })

  override def pullAndUpdateStatus(taskType: String, size: Int, taskStatus: String): Future[Seq[Task]] =
    Future.successful(DB.localTx { implicit session =>
      val tasks =
        sql"select * from projects_url pu, projects p where p.type = ${taskType} and pu.status = ${TaskStatus.taskWait} and p.id = pu.project_id limit ${size}"
          .map(
            rs =>
              Task(
                id = rs.get("id"),
                taskType = rs.get("type"),
                taskData = rs.get("url")
            ))
          .list()
          .apply()
      val tasksIds = tasks.map(_.id)
      sql"update projects_url set status = ${taskStatus} where id in (${tasksIds})".update().apply()
      tasks
    })

  override def pushTasks(taskData: Map[String, Seq[String]]): Future[Unit] = Future.successful()

  override def updateTasksStatus(ids: Seq[String], taskStatus: String): Future[Unit] =
    Future.successful(DB.localTx { implicit session =>
      val tasksIds = ids.map(Integer.parseInt)
      sql"update projects_url set status = ${taskStatus} where id in (${tasksIds})".update().apply()
    })

  override def updateTasksStatusFromTo(time: Instant, fromStatus: String, toStatus: String): Future[Long] =
    Future.successful(DB.localTx { implicit session =>
      sql"update projects_url set status = ${toStatus} where status = ${fromStatus}".update().apply()
    })

  override def updateTasksStatusAndIncAttempt(ids: Seq[String], taskStatus: String): Future[Unit] =
    updateTasksStatus(ids, taskStatus)

  override def dropTasks(ids: Seq[String]): Future[Unit] =
    Future.successful(DB.localTx { implicit session =>
      val tasksIds = ids.map(Integer.parseInt)
      sql"update projects_url set status = ${TaskStatus.} where id in (${tasksIds})"
        .executeUpdate()
        .apply()
    })

  override def getByIds(ids: Seq[String]): Future[Seq[Task]] =
    Future.successful(DB.localTx { implicit session =>
      val tasksIds = ids.map(Integer.parseInt)
      sql"select * from projects_url where id in (${tasksIds})"
        .map(
          rs =>
            Task(
              id = rs.get("id"),
              taskType = rs.get("type"),
              taskData = rs.get("url")
          ))
        .list()
        .apply()
    })
}
