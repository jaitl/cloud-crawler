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
      sql"select pu.id, pu.url, pu.project_id, p.next_project_id, p.base_domain from projects_url pu, projects p where p.type = ${taskType} and p.id = pu.project_id and pu.status = ${TaskStatus.taskWait} limit ${size}"
        .map(
          rs =>
            Task(
              id = rs.get("id"),
              taskData = rs.get("url"),
              projectId = rs.get("project_id"),
              nextProjectId = rs.get("next_project_id"),
              baseDomain = rs.get("base_domain")
          ))
        .list()
        .apply()
    })

  override def pullAndUpdateStatus(taskType: String, size: Int, taskStatus: String): Future[Seq[Task]] =
    Future.successful(DB.localTx { implicit session =>
      val tasks =
        sql"select pu.id, pu.url, pu.project_id, p.next_project_id, p.base_domain from projects_url pu, projects p where p.type = ${taskType} and pu.status = ${TaskStatus.taskWait} and p.id = pu.project_id limit ${size}"
          .map(
            rs =>
              Task(
                id = rs.get("id"),
                taskData = rs.get("url"),
                projectId = rs.get("project_id"),
                nextProjectId = rs.get("next_project_id"),
                baseDomain = rs.get("base_domain")
              ))
          .list()
          .apply()
      val tasksIds = tasks.map(_.id)
      sql"update projects_url set status = $taskStatus where id in (${tasksIds})".update().apply()
      tasks
    })

  override def pushTasks(taskData: Seq[Task]): Future[Unit] =
    Future.successful(DB.localTx { implicit session =>
      val urls = taskData.map(_.taskData)
      val tasks =
        sql"select pu.url from projects_url pu where pu.url in (${urls})"
          .map(
            rs =>
              Task(
                id = rs.get("id"),
                taskData = rs.get("url"),
                projectId = rs.get("project_id"),
                nextProjectId = rs.get("next_project_id"),
                baseDomain = rs.get("base_domain")
              ))
          .list()
          .apply().map(_.taskData)
      taskData.filter(t => !tasks.contains(t.taskData)).foreach(t =>
        sql"insert into projects_url(`url`, `status`, `project_id`) values (${t.taskData}, ${TaskStatus.taskWait}, ${t.nextProjectId})"
          .update()
          .apply())
    })

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
      sql"update projects_url set status = ${TaskStatus.taskFinished} where id in (${tasksIds})"
        .executeUpdate()
        .apply()
    })

  override def getByIds(ids: Seq[String]): Future[Seq[Task]] =
    Future.successful(DB.localTx { implicit session =>
      val tasksIds = ids.map(Integer.parseInt)
      sql"select pu.id, pu.url, pu.project_id, p.next_project_id, p.base_domain from projects_url pu, project p where id in (${tasksIds}) and pu.project_id = p.id"
        .map(
          rs =>
            Task(
              id = rs.get("id"),
              taskData = rs.get("url"),
              projectId = rs.get("project_id"),
              nextProjectId = rs.get("next_project_id"),
              baseDomain = rs.get("base_domain")
            ))
        .list()
        .apply()
    })
}
