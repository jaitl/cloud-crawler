package com.github.jaitl.crawler.worker.save

import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticProperties

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


class ElasticSearchSaveParsedProvider[T](
  server: String,
  index: String,
  port: Int,
  clusername: String
)(implicit converter: ElasticSearchTypeConverter[T]) extends SaveParsedProvider[T] {
  import com.sksamuel.elastic4s.http.ElasticDsl._ // scalastyle:off

  val client = ElasticClient(ElasticProperties(s"http://$server:$port?cluster.name=$clusername"))

  override def saveResults(parsedData: Seq[T])(implicit executionContext: ExecutionContext): Future[Unit] = Future {
    parsedData.map(d => {
      val doc = converter.convert(d)
      client.execute {
        indexInto(index / index).source(doc)
      }.await
    })
  }
}
