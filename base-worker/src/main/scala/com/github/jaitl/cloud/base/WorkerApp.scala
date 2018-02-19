package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.cloud.base.exception.NoPipelinesException
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.QueueTaskBalancerSingletonMessages
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object WorkerApp extends StrictLogging {

  private var piplines: Option[Seq[Pipeline]] = None

  def addPiplines(piplines: Seq[Pipeline]): Unit = {
    this.piplines = Some(piplines)
  }

  def run(): Unit = {
    piplines match {
      case None | Some(pip) if pip.isEmpty => throw new NoPipelinesException
      case Some(pip) => logger.info(s"count piplines: ${pip.length}")
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

    queueTaskBalancer ! QueueTaskBalancerSingletonMessages.GetWork(UUID.randomUUID(), Seq("type1", "type2"))
    queueTaskBalancer ! QueueTaskBalancerSingletonMessages.GetWork(UUID.randomUUID(), Seq("type1", "type2"))
  }
}
