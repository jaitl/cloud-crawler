package com.github.jaitl.crawler.master.config

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.cluster.sharding.ShardRegion
import akka.pattern.pipe
import com.github.jaitl.crawler.master.config.ConfigurationRequestController.ConfigurationRequestFailure
import com.github.jaitl.crawler.master.config.ConfigurationRequestController.ConfigurationRequestSuccess
import com.github.jaitl.crawler.master.config.ConfigurationRequestController.RequestConfiguration
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProvider
import com.github.jaitl.crawler.models.worker.ProjectConfiguration
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureConfigRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoConfigs
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksConfigRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

class ConfigurationRequestController(
  configurationProvider: CrawlerConfigurationProvider
) extends Actor
    with ActorLogging
    with Stash {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def receive: Receive = waitRequest

  private def waitRequest: Receive = {
    case RequestConfiguration(requestId, taskType, requester) =>
      log.debug(s"RequestConfiguration: $requestId, $taskType, self: $self")

      context.become(processingRequest)

      val configResult = for {
        configs <- configurationProvider.getCrawlerProjectConfiguration(taskType)
        _ <- if (configs.nonEmpty) {
          Future.successful(Unit)
        } else {
          Future.failed(NoConfigurationFound(s"No configuration for $taskType"))
        }
      } yield ConfigurationRequestSuccess(requestId, taskType, requester, configs)

      configResult
        .recover { case t: Throwable => ConfigurationRequestFailure(requestId, taskType, requester, t) }
        .pipeTo(self)
  }

  private def processingRequest: Receive = {
    case ConfigurationRequestSuccess(requestId, taskType, requester, tasks) =>
      log.info(s"ConfigurationRequestSuccess: $requestId, $taskType, self: $self, tasks: $tasks")

      if (tasks.nonEmpty) {
        requester ! SuccessTasksConfigRequest(requestId, taskType, tasks.head)
      } else {
        requester ! NoConfigs(requestId, taskType)
      }

      context.unbecome()
      unstashAll()

    case ConfigurationRequestFailure(requestId, taskType, requester, throwable) =>
      requester ! FailureConfigRequest(requestId, taskType, throwable)

      context.unbecome()
      unstashAll()

    case req @ RequestConfiguration(_, _, _) =>
      log.debug(s"stash req: $req")
      stash()
  }
  final case class NoConfigurationFound(private val message: String = "", private val cause: Throwable = None.orNull)
      extends Exception(message, cause)
}

object ConfigurationRequestController {

  case class RequestConfiguration(requestId: UUID, taskType: String, requester: ActorRef)

  case class ConfigurationRequestSuccess(
    requestId: UUID,
    taskType: String,
    requester: ActorRef,
    config: Seq[ProjectConfiguration])

  case class ConfigurationRequestFailure(requestId: UUID, taskType: String, requester: ActorRef, throwable: Throwable)

  def props(configurationProvider: CrawlerConfigurationProvider): Props =
    Props(new ConfigurationRequestController(configurationProvider))

  def name(): String = "configurationRequestController"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ RequestConfiguration(_, taskType, _) => (taskType, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case RequestConfiguration(_, taskType, _) => taskType
  }
}
