package com.github.jaitl.cloud.base.http

import scala.concurrent.Future

trait HttpRequestExecutor {
  def get(uri: String): Future[HttpResult]
  def postJson(uri: String, jsonData: String): Future[HttpResult]
}

case class HttpResult(code: Int, body: String)
