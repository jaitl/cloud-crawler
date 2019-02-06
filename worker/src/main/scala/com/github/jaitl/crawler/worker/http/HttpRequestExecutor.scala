package com.github.jaitl.crawler.worker.http

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait HttpRequestExecutor extends AutoCloseable {
  def getExecutorId(): UUID
  def get(uri: String): Future[HttpResult]
  def postJson(uri: String, jsonData: String): Future[HttpResult]
}

object HttpRequestExecutor {
  def getExecutor(config: HttpRequestExecutorConfig)(implicit executionContext: ExecutionContext): HttpRequestExecutor =
    new AsyncHttpRequestExecutor(config)(executionContext)
}

case class HttpResult(code: Int, body: String)

trait ProxyType

object ProxyType {
  object Http extends ProxyType
  object Socks5 extends ProxyType
}

case class HttpRequestExecutorConfig(
  executorId: UUID,
  host: String,
  port: Int,
  proxyType: ProxyType,
  userAgent: String,
  login: String = "",
  password: String = ""
)
