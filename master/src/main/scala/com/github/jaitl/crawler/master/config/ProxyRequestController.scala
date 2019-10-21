package com.github.jaitl.crawler.master.config

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.pattern.pipe
import com.github.jaitl.crawler.master.config.ProxyRequestController.ProxyRequestFailure
import com.github.jaitl.crawler.master.config.ProxyRequestController.ProxyRequestSuccess
import com.github.jaitl.crawler.master.config.ProxyRequestController.RequestProxy
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProvider
import com.github.jaitl.crawler.models.worker.CrawlerProxy
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureProxyRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoProxies
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessProxyRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

class ProxyRequestController(
  proxyProvider: CrawlerConfigurationProvider
) extends Actor
    with ActorLogging
    with Stash {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def receive: Receive = waitRequest

  private def waitRequest: Receive = {
    case RequestProxy(requestId, taskType, requester) =>
      log.debug(s"RequestProxy: $requestId, $taskType, self: $self")
      val proxyResult = for {
        proxies <- proxyProvider.getCrawlerProxyConfiguration(taskType)
        _ <- if (proxies.isEmpty) {
          Future.successful(Unit)
        } else {
          Future.failed(NoProxyFound(s"No proxies for $taskType"))
        }
      } yield ProxyRequestSuccess(requestId, taskType, requester, proxies)

      proxyResult
        .recover { case t: Throwable => ProxyRequestFailure(requestId, taskType, requester, t) }
        .pipeTo(self)
  }

  private def processingRequest: Receive = {
    case ProxyRequestSuccess(requestId, taskType, requester, proxies) =>
      log.info(s"ProxyRequestSuccess: $requestId, $taskType, self: $self, proxies: $proxies")

      if (proxies.nonEmpty) {
        requester ! SuccessProxyRequest(requestId, taskType, proxies.head)
      } else {
        requester ! NoProxies(requestId, taskType)
      }

      context.unbecome()
      unstashAll()

    case ProxyRequestFailure(requestId, taskType, requester, throwable) =>
      requester ! FailureProxyRequest(requestId, taskType, throwable)

      context.unbecome()
      unstashAll()

    case req @ RequestProxy(_, _, _) =>
      log.debug(s"stash req: $req")
      stash()
  }

  final case class NoProxyFound(private val message: String = "", private val cause: Throwable = None.orNull)
      extends Exception(message, cause)
}
object ProxyRequestController {

  case class RequestProxy(requestId: UUID, taskType: String, requester: ActorRef)

  case class ProxyRequestSuccess(requestId: UUID, taskType: String, requester: ActorRef, config: Seq[CrawlerProxy])

  case class ProxyRequestFailure(requestId: UUID, taskType: String, requester: ActorRef, throwable: Throwable)

  def props(configurationProvider: CrawlerConfigurationProvider): Props =
    Props(new ProxyRequestController(configurationProvider))

  def name(): String = "proxyRequestController"
}
