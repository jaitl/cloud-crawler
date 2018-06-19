package com.github.jaitl.crawler.worker.save

import scala.concurrent.Future

class MongoSaveParsedProvider[T](val collectionName: String) extends SaveParsedProvider[T] {
  override def saveResults[T](parsedData: Seq[T]): Future[Unit] = ???
}
