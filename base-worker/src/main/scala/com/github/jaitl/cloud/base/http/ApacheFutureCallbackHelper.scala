package com.github.jaitl.cloud.base.http

import org.apache.http.HttpResponse
import org.apache.http.concurrent.FutureCallback

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.io.Source
import scala.util.Try

class ApacheFutureCallbackHelper extends FutureCallback[HttpResponse] {
  private val promise: Promise[HttpResult] = Promise[HttpResult]()

  override def failed(ex: Exception): Unit = promise.failure(ex)

  override def completed(result: HttpResponse): Unit = {
    val stringResult = Try(Source.fromInputStream(result.getEntity.getContent).mkString(""))

    val httpResult = HttpResult(result.getStatusLine.getStatusCode, stringResult.getOrElse(""))

    promise.success(httpResult)
  }

  override def cancelled(): Unit = promise.failure(new RequestCancelledException)

  def getFuture: Future[HttpResult] = promise.future
}

class RequestCancelledException extends Exception
