package com.github.jaitl.crawler.master.config.provider

import com.github.jaitl.crawler.master.client.configuration.ProjectConfiguration
import com.github.jaitl.crawler.master.client.configuration.ProxyResource
import com.github.jaitl.crawler.master.client.configuration.TorResource
import scalikejdbc.ConnectionPool
import scalikejdbc._

import scala.concurrent.Future
import scala.util.Random

class SqlConfigurationProvider(
  driverName: String,
  connectionUrl: String,
  user: String,
  password: String
) extends CrawlerConfigurationProvider {
  Class.forName(driverName)
  ConnectionPool.singleton(connectionUrl, user, password)

  override def getCrawlerProjectConfiguration(taskType: String): Future[Seq[ProjectConfiguration]] =
    Future.successful(DB.localTx { implicit session =>
      sql"select * from projects p where p.type = ${taskType}"
        .map(rs =>
          ProjectConfiguration(
            id = rs.get("id"),
            workerExecuteInterval = rs.get("worker_execute_interval"),
            workerFilePath = rs.get("worker_file_path"),
            workerParallelBatches = Integer.parseInt(rs.get("worker_parallel_batches")),
            workerTaskType = rs.get("type"),
            workerBatchSize = Integer.parseInt(rs.get("worker_batch_size")),
            workerResource = rs.get("worker_resource"),
            notification = rs.get("worker_notification")
        ))
        .list()
        .apply()
    })

  override def getCrawlerProxyConfiguration(taskType: String): Future[Seq[ProxyResource]] =
    Future.successful(DB.localTx { implicit session =>
      Random.shuffle(
        sql"select * from projects_endpoints p where p.type = 'Proxy'"
          .map(rs =>
            ProxyResource(
              id = rs.get("id"),
              workerProxyHost = rs.get("host"),
              workerProxyPort = rs.get("port"),
              workerProxyTimeoutUp = rs.get("timeout_up"),
              workerProxyTimeoutDown = rs.get("timeout_down"),
              workerParallel = rs.get("concurrency"),
              workerProxyLogin = rs.get("login"),
              workerProxyPassword = rs.get("password"),
              workerTaskType = Seq("HTML")
            ))
          .list()
          .apply()
      )
    })

  override def getCrawlerTorConfiguration(taskType: String): Future[Seq[TorResource]] =
    Future.successful(DB.localTx { implicit session =>
      Random.shuffle(
        sql"select * from projects_endpoints p where p.type = 'Tor'"
          .map(rs =>
            TorResource(
              id = rs.get("id"),
              workerTorHost = rs.get("host"),
              workerTorPort = rs.get("port"),
              workerTorTimeoutUp = rs.get("timeout_up"),
              workerTorTimeoutDown = rs.get("timeout_down"),
              workerTorLimit = rs.get("concurrency"),
              workerTorPassword = rs.get("control_password"),
              workerTorControlPort = rs.get("control_port"),
              workerTaskType = Seq("HTML")
          ))
          .list()
          .apply()
      )
    })
}
