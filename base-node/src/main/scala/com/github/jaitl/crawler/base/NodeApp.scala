package com.github.jaitl.crawler.base

import akka.actor.ActorSystem
import com.github.jaitl.crawler.base.master.MasterApp
import com.github.jaitl.crawler.base.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.base.worker.WorkerApp
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import com.typesafe.scalalogging.StrictLogging

object NodeApp extends StrictLogging {
  import scala.collection.JavaConverters._ // scalastyle:ignore

  private val actorSystemName: String = "cloudCrawlerSystem"

  private var pipelines: Option[Map[String, Pipeline]] = None
  private var parallelBatches: Option[Int] = Some(2)
  private var taskProvider: Option[QueueTaskProvider] = None

  def addPipelines(pipelines: Seq[Pipeline]): this.type = {
    this.pipelines = Some(pipelines.map(pipe => pipe.taskType -> pipe).toMap)
    this
  }

  def addParallelBatches(limitParallelBatches: Int): this.type = {
    this.parallelBatches = Some(limitParallelBatches)
    this
  }

  def addTaskProvider(taskProvider: QueueTaskProvider): this.type = {
    this.taskProvider = Some(taskProvider)
    this
  }

  def run(): Unit = {
    val baseNodeConfig = ConfigFactory.load("base-node.conf")

    val roles = baseNodeConfig.getString("node.roles").split(",").toSeq
    val seeds = baseNodeConfig.getString("node.seeds").split(",")
      .map(address => s"akka.tcp://$actorSystemName@$address")
      .toSeq

    val fullNodeConfig = baseNodeConfig
      .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(roles.asJava))
      .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds.asJava))

    logger.info(
      "Start node on {}:{}, with roles: {}",
      fullNodeConfig.getConfig("akka.remote.netty.tcp").getString("hostname"),
      fullNodeConfig.getConfig("akka.remote.netty.tcp").getString("port"),
      fullNodeConfig.getConfig("node").getString("roles")
    )

    val system = ActorSystem(actorSystemName, fullNodeConfig)

    try {
      if (roles.contains("master")) {
        new MasterApp(system, taskProvider.get).run()
      }

      if (roles.contains("worker")) {
        new WorkerApp(pipelines.get, parallelBatches.get, system).run()
      }
    } catch {
      case t: Throwable =>
        logger.error("Error during run node", t)
        system.terminate()
    }
  }
}
