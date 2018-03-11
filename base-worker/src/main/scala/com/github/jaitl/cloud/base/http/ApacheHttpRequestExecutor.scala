package com.github.jaitl.cloud.base.http

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.StringEntity
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients

import scala.concurrent.Future

class ApacheHttpRequestExecutor extends HttpRequestExecutor {
  private val httpclient: CloseableHttpAsyncClient = HttpAsyncClients.createDefault

  def get(uri: String): Future[HttpResult] = {
    val getRequest = new HttpGet(uri)

    executeRequest(getRequest)
  }

  def postJson(uri: String, jsonData: String): Future[HttpResult] = {
    val postRequest = new HttpPost()

    val params = new StringEntity(jsonData)
    postRequest.addHeader("content-type", "application/json")
    postRequest.setEntity(params)

    executeRequest(postRequest)
  }

  private def executeRequest(request: HttpRequestBase): Future[HttpResult] = {
    val callback = new ApacheFutureCallbackHelper()

    httpclient.execute(request, callback)

    callback.getFuture
  }
}
