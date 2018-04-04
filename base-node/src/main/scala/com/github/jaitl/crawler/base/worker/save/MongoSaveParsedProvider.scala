package com.github.jaitl.crawler.base.worker.save

import java.util.concurrent.Future

import com.github.jaitl.crawler.base.worker.parser.ParsedData

class MongoSaveParsedProvider[T <: ParsedData](val collectionName: String) extends SaveParsedProvider[T] {
  override def saveResults(parsedData: Seq[T]): Future[Unit] = ???
}
