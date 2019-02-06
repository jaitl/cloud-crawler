package com.github.jaitl.crawler.worker.save

import com.sksamuel.elastic4s.http._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.sksamuel.elastic4s.http.ElasticDsl._


class ElasticSearchSaveParsedProvider[T](
  server: String,
  index: String,
  port: Int,
  clusername: String
)(implicit converter: ElasticSearchTypeConverter[T]) extends SaveParsedProvider[T] {
  val client = ElasticClient(ElasticProperties(s"http://$server:$port?cluster.name=$clusername"))

  override def saveResults(parsedData: Seq[T])(implicit executionContext: ExecutionContext): Future[Unit] = Future {
    parsedData.par.map(d => {
      val doc = converter.convert(d)
      client.execute {
        indexInto(index / index).source(doc)
      }.await
    })
  }
}
