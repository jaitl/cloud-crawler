package com.github.jaitl.crawler.base.worker.save

import java.util.concurrent.Future

class MongoSaveParsedProvider[T](val collectionName: String) extends SaveParsedProvider[T] {
  override def saveResults(parsedData: Seq[T]): Future[Unit] = ???
}
