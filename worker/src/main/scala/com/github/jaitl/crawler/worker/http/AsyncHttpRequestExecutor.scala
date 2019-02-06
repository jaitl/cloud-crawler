package com.github.jaitl.crawler.worker.http

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpHeaderNames
import org.asynchttpclient.{AsyncHttpClient, BoundRequestBuilder, Realm}
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.Dsl.config
import org.asynchttpclient.proxy.ProxyServer

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AsyncHttpRequestExecutor(
  executorConfig: HttpRequestExecutorConfig
)(implicit executionContext: ExecutionContext) extends HttpRequestExecutor with StrictLogging {
  private val jsonContentType = "application/json"
  private val maxRedirectsCount = 5

  private val client: AsyncHttpClient = {
    val proxyType = executorConfig.proxyType match {
      case ProxyType.Socks5 => org.asynchttpclient.proxy.ProxyType.SOCKS_V5
      case ProxyType.Http => org.asynchttpclient.proxy.ProxyType.HTTP
    }

    val proxyServer = executorConfig.login match  {
      case "" => new ProxyServer.Builder(executorConfig.host, executorConfig.port).setProxyType(proxyType).build()
      case _ =>
        new ProxyServer.Builder(executorConfig.host, executorConfig.port)
          .setProxyType(proxyType)
          .setRealm(new Realm.Builder(executorConfig.login, executorConfig.password)
          .setScheme(Realm.AuthScheme.BASIC))
          .build()
    }


    asyncHttpClient(config().setProxyServer(proxyServer).setUserAgent(executorConfig.userAgent)
      .setFollowRedirect(true).setMaxRedirects(maxRedirectsCount))
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
