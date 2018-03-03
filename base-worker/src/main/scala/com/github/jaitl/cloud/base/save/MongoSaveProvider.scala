package com.github.jaitl.cloud.base.save
import java.util.concurrent.Future

class MongoSaveProvider extends SaveProvider {
  override def saveResults(parsedData: Map[String, String]): Future[Unit] = ???
}
