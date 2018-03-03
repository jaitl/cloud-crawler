package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.cloud.base.exception.NoPipelinesException
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.request.RequestTasksBatch
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object WorkerApp extends StrictLogging {

  private var piplines: Option[Seq[Pipeline]] = None

  def addPiplines(piplines: Seq[Pipeline]): this.type = {
    this.piplines = Some(piplines)

    this
  }

  def run(): Unit = {
    piplines match {
      case Some(pip) if pip.nonEmpty => logger.info(s"count piplines: ${pip.length}")
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

    queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), Seq("type1", "type2"))
    queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), Seq("type1", "type2"))
  }
}
