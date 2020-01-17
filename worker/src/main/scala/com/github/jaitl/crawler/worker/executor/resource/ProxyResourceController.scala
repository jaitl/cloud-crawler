package com.github.jaitl.crawler.worker.executor.resource

import java.time.Instant
import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import com.github.jaitl.crawler.worker.executor.resource.ProxyResourceController.ExecutorContext
import com.github.jaitl.crawler.worker.executor.resource.ProxyResourceController.ProxyConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.NoFreeResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.RequestResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnBannedResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnFailedResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnSkippedResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnSkippedResourceNoWait
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnSuccessResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor
import com.github.jaitl.crawler.worker.http.HttpRequestExecutorConfig
import com.github.jaitl.crawler.worker.http.ProxyType
import com.github.jaitl.crawler.worker.http.agent.UserAgentGenerator
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

import scala.collection.mutable
import scala.concurrent.ExecutionContext
//scalastyle:off
private class ProxyResourceController(
  config: ProxyConfig
) extends Actor
    with ActorLogging {
  implicit private val executionContext: ExecutionContext = context.dispatcher

  val executors: mutable.Map[UUID, ExecutorContext] = mutable.Map.empty

  var failCount: Int = 0

  override def postStop(): Unit = {
    super.postStop()
    executors.values.foreach(_.executor.close())
  }

  override def receive: Receive = {
    case RequestResource(requestId) =>
      if (failCount >= config.maxFailCount) {
        sender() ! NoResourcesAvailable(requestId)
      } else if (executors.size < config.limit) {
        val context = createNewExecutorContext()
        executors += context.id -> context.copy(isUsed = true)

        sender() ! SuccessRequestResource(requestId, context.executor)
      } else {
        val now = Instant.now()
        val context = executors.values.find(c => !c.isUsed && c.awaitTo.forall(t => t.isBefore(now)))

        context match {
          case Some(c) =>
            executors += c.id -> c.copy(isUsed = true)
            log.info(s"Get executor Id ${c.id} with ${c.awaitTo}")
            sender() ! SuccessRequestResource(requestId, c.executor)
          case None =>
            sender() ! NoFreeResource(requestId)
        }
      }

    case ReturnSuccessResource(requestId, requestExecutor) =>
      val now = Instant.now()
      val awaitTo = now.plusMillis(config.timeout.computeRandom.toMillis)
      log.info(s"Waiting in ReturnSuccessResource $requestId for $awaitTo")
      val context = executors(requestExecutor.getExecutorId())
        .copy(isUsed = false, awaitTo = Some(awaitTo))
      executors += context.id -> context
      if (failCount > 0) {
        failCount = failCount - 1
      }

    case ReturnFailedResource(requestId, requestExecutor, t) =>
      val now = Instant.now()
      val awaitTo = now.plusMillis(config.timeout.computeRandom.toMillis)
      log.info(s"Waiting in ReturnFailedResource $requestId for $awaitTo")
      val context = executors(requestExecutor.getExecutorId())
        .copy(isUsed = false, awaitTo = Some(awaitTo))
      executors += context.id -> context
      failCount = failCount + 1

    case ReturnSkippedResource(requestId, requestExecutor, t) =>
      val now = Instant.now()
      val awaitTo = now.plusMillis(config.timeout.computeRandom.toMillis)
      val context = executors(requestExecutor.getExecutorId())
        .copy(isUsed = false, awaitTo = Some(awaitTo))
      executors += context.id -> context
      log.info(s"Waiting in ReturnSkippedResource $requestId for $awaitTo")

    case ReturnSkippedResourceNoWait(requestId, requestExecutor) =>
      val now = Instant.now()
      val awaitTo = now.plusSeconds(1)
      val context = executors(requestExecutor.getExecutorId())
        .copy(isUsed = false, awaitTo = Some(awaitTo))
      executors += context.id -> context
      log.info(s"Waiting in ReturnSkippedResourceNoWait $requestId for $awaitTo")

    case ReturnBannedResource(requestId, requestExecutor, t) =>
      val now = Instant.now()
      val awaitTo = now.plusSeconds(30 * 60)
      val context = executors(requestExecutor.getExecutorId())
        .copy(isUsed = false, awaitTo = Some(awaitTo))
      executors += context.id -> context
      log.info(s"Waiting in ReturnBannedResource $requestId for $awaitTo")
  }

  def createNewExecutorContext(): ExecutorContext = {
    val id = UUID.randomUUID()
    val userAgent = UserAgentGenerator.randomUserAgent()
    val executorConfig = HttpRequestExecutorConfig(
      executorId = id,
      host = config.host,
      port = config.port,
      proxyType = ProxyType.Http,
      userAgent = userAgent,
      login = config.login,
      password = config.password
    )

    val executor = HttpRequestExecutor.getExecutor(executorConfig)
    ExecutorContext(
      id = id,
      executor = executor,
      lastUsage = Instant.now(),
      isUsed = false,
      awaitTo = None
    )
  }
}

private object ProxyResourceController {
  case class ExecutorContext(
    id: UUID,
    executor: HttpRequestExecutor,
    lastUsage: Instant,
    isUsed: Boolean,
    awaitTo: Option[Instant]
  )
  case class ProxyConfig(
    host: String,
    port: Int,
    limit: Int,
    maxFailCount: Int,
    timeout: RandomTimeout,
    login: String = "",
    password: String = "")

  def props(config: ProxyConfig): Props =
    Props(new ProxyResourceController(config))
  def name: String = s"proxyResourceController"
}
