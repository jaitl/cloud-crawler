package com.github.jaitl.crawler.worker.http

import java.util.UUID

import io.netty.handler.codec.http.HttpHeaderNames
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.Dsl.config
import org.asynchttpclient.proxy.ProxyServer

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AsyncHttpRequestExecutor(
  executorConfig: HttpRequestExecutorConfig
)(implicit executionContext: ExecutionContext) extends HttpRequestExecutor {
  private val jsonContentType = "application/json"

  private val client: AsyncHttpClient = {
    val proxyType = executorConfig.proxyType match {
      case ProxyType.Socks5 => org.asynchttpclient.proxy.ProxyType.SOCKS_V5
      case ProxyType.Http => org.asynchttpclient.proxy.ProxyType.HTTP
    }

    val proxyServer = new ProxyServer.Builder(executorConfig.host, executorConfig.port).setProxyType(proxyType).build()

    asyncHttpClient(config().setProxyServer(proxyServer).setUserAgent(executorConfig.userAgent))
  }

  override def get(uri: String): Future[HttpResult] = {
    val getRequest = client.prepareGet(uri)

    execute(getRequest)
  }

  override def postJson(uri: String, jsonData: String): Future[HttpResult] = {
    val postRequest: BoundRequestBuilder = client.preparePost(uri)
      .setBody(jsonData)
      .setHeader(HttpHeaderNames.CONTENT_TYPE, jsonContentType)

    execute(postRequest)
  }

  private def execute(request: BoundRequestBuilder): Future[HttpResult] = {
    toScala(request.execute().toCompletableFuture).map(resp => HttpResult(resp.getStatusCode, resp.getResponseBody))
  }

  override def getExecutorId(): UUID = executorConfig.executorId

  override def close(): Unit = {
    client.close()
  }
}
