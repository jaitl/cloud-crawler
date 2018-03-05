package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import akka.routing.RoundRobinPool
import com.github.jaitl.cloud.base.config.WorkerConfig
import com.github.jaitl.cloud.base.exception.NoPipelinesException
import com.github.jaitl.cloud.base.executor.CrawlExecutor
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.request.RequestTasksBatch
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object WorkerApp extends StrictLogging {

  private var pipelines: Option[Seq[Pipeline]] = None

  private var parallelBatch: Option[Int] = None

  def addPipelines(pipelines: Seq[Pipeline]): this.type = {
    this.pipelines = Some(pipelines)

    this
  }

  def run(): Unit = {
    pipelines match {
      case Some(pip) if pip.nonEmpty => logger.info(s"count pipelines: ${pip.length}")
      case _ => throw new NoPipelinesException
    }

    val config = ConfigFactory.load()

    logger.info("Start worker on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port"))


    val system = ActorSystem("cloudCrawlerSystem", config)

    val queueTaskBalancer: ActorRef = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/queueTaskBalancer",
        settings = ClusterSingletonProxySettings(system).withRole("master")),
      name = "queueTaskBalancerProxy")

    val workerConfig = WorkerConfig(parallelBatch.getOrElse(2))

    val executorCount = 100

    val crawlExecutorRouter = system.actorOf(
      RoundRobinPool(executorCount).props(CrawlExecutor.props()),
      "crawlExecutorRouter"
    )

    val workerManager = system.actorOf(
      WorkerManager.props(queueTaskBalancer, crawlExecutorRouter, pipelines.get, workerConfig),
      WorkerManager.name()
    )

    workerManager ! WorkerManager.RequestBatch
  }
}
