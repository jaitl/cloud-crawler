package com.github.jaitl.crawler.base.worker.save

import java.util.concurrent.Future

class MongoSaveParsedProvider(val collectionName: String) extends SaveParsedProvider {
  override def saveResults(parsedData: Map[String, String]): Future[Unit] = ???
}
