package com.github.jaitl.crawler.master

import akka.actor.ActorSystem
import com.github.jaitl.crawler.master.config.ConfigurationServiceImpl
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProvider
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProviderFactory
import com.github.jaitl.crawler.master.queue.QueueTaskConfig
import com.github.jaitl.crawler.master.queue.QueueTaskRecover
import com.github.jaitl.crawler.master.queue.QueueTaskRecover.RecoveryConfig
import com.github.jaitl.crawler.master.queue.QueueTaskServiceImpl
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProviderFactory
import com.github.jaitl.crawler.master.scheduler.AkkaScheduler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import io.grpc.ServerBuilder

import scala.concurrent.ExecutionContext

object MasterApp extends StrictLogging {
  import net.ceedubs.ficus.Ficus._ // scalastyle:ignore
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._ // scalastyle:ignore

  // scalastyle:off method.length
  def main(args: Array[String]): Unit = {
    val executionContextGlobal: ExecutionContext = ExecutionContext.global

    val config = ConfigFactory.load("master.conf")

    val taskProvider: QueueTaskProvider = QueueTaskProviderFactory
      .getProvider(config.getConfig("master.task-provider"))

    val configProvider: CrawlerConfigurationProvider = CrawlerConfigurationProviderFactory
      .getProvider(config.getConfig("master.config-provider"))

    val system = ActorSystem("CCMasterActorSystem", config)

    val queueTaskConfig = config.as[QueueTaskConfig]("master.queue-task")
    val recoveryConfig = config.as[RecoveryConfig]("master.queue-task-recovery")

    val grpcConfigPort = config.getInt("master.config-provider.grpc.port")
    val server = ServerBuilder
      .forPort(grpcConfigPort)
      .addService(new ConfigurationServiceImpl(configProvider)(executionContextGlobal))
      .addService(new QueueTaskServiceImpl(taskProvider, queueTaskConfig)(executionContextGlobal))
      .build()

    server.start()
    logger.info(s"Configuration gRPC service started on port: $grpcConfigPort")

    val taskRecover = system.actorOf(
      props = QueueTaskRecover.props(taskProvider, new AkkaScheduler(system), recoveryConfig),
      name = QueueTaskRecover.name())
  }
}
