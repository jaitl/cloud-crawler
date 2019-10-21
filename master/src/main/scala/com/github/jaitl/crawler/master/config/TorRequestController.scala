package com.github.jaitl.crawler.master.config

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.pattern.pipe
import com.github.jaitl.crawler.master.config.ConfigurationRequestController.RequestConfiguration
import com.github.jaitl.crawler.master.config.TorRequestController.RequestTor
import com.github.jaitl.crawler.master.config.TorRequestController.TorRequestFailure
import com.github.jaitl.crawler.master.config.TorRequestController.TorRequestSuccess
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProvider
import com.github.jaitl.crawler.models.worker.CrawlerTor
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureTorRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoConfigs
import com.github.jaitl.crawler.models.worker.WorkerManager.NoTors
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksConfigRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTorRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

class TorRequestController(
  configurationProvider: CrawlerConfigurationProvider
) extends Actor
    with ActorLogging
    with Stash {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def receive: Receive = waitRequest

  private def waitRequest: Receive = {
    case RequestTor(requestId, taskType, requester) =>
      log.debug(s"RequestConfiguration: $requestId, $taskType, self: $self")
      val torsResult = for {
        tors <- configurationProvider.getCrawlerTorConfiguration(taskType)
        _ <- if (tors.isEmpty) {
          Future.successful(Unit)
        } else {
          Future.failed(NoTorsFound(s"No tors for $taskType"))
        }
      } yield TorRequestSuccess(requestId, taskType, requester, tors)

      torsResult
        .recover { case t: Throwable => TorRequestFailure(requestId, taskType, requester, t) }
        .pipeTo(self)
  }

  private def processingRequest: Receive = {
    case TorRequestSuccess(requestId, taskType, requester, tors) =>
      log.info(s"ConfigurationRequestSuccess: $requestId, $taskType, self: $self, tors: $tors")

      if (tors.nonEmpty) {
        requester ! SuccessTorRequest(requestId, taskType, tors.head)
      } else {
        requester ! NoTors(requestId, taskType)
      }

      context.unbecome()
      unstashAll()

    case TorRequestFailure(requestId, taskType, requester, throwable) =>
      requester ! FailureTorRequest(requestId, taskType, throwable)

      context.unbecome()
      unstashAll()

    case req @ RequestConfiguration(_, _, _) =>
      log.debug(s"stash req: $req")
      stash()
  }
  final case class NoTorsFound(private val message: String = "", private val cause: Throwable = None.orNull)
      extends Exception(message, cause)
}
object TorRequestController {

  case class RequestTor(requestId: UUID, taskType: String, requester: ActorRef)

  case class TorRequestSuccess(requestId: UUID, taskType: String, requester: ActorRef, config: Seq[CrawlerTor])

  case class TorRequestFailure(requestId: UUID, taskType: String, requester: ActorRef, throwable: Throwable)

  def props(configurationProvider: CrawlerConfigurationProvider): Props =
    Props(new TorRequestController(configurationProvider))

  def name(): String = "torRequestController"
}
