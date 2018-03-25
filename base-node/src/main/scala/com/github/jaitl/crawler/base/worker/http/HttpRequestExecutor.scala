package com.github.jaitl.crawler.base.worker.http

import scala.concurrent.Future

trait HttpRequestExecutor {
  def get(uri: String): Future[HttpResult]
  def postJson(uri: String, jsonData: String): Future[HttpResult]
}

case class HttpResult(code: Int, body: String)
