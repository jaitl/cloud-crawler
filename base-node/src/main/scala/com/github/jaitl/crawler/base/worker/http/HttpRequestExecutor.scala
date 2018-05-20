package com.github.jaitl.crawler.base.worker.http

import scala.concurrent.Future

trait HttpRequestExecutor extends AutoCloseable {
  def get(uri: String): Future[HttpResult]
  def postJson(uri: String, jsonData: String): Future[HttpResult]
}

case class HttpResult(code: Int, body: String)

trait ProxyType

object ProxyType {
  object Http extends ProxyType
  object Socks5 extends ProxyType
}

case class HttpRequestExecutorConfig(
  host: String,
  port: Int,
  proxyType: ProxyType,
  userAgent: String
)
