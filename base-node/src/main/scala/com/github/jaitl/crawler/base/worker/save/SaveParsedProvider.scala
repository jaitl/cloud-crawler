package com.github.jaitl.crawler.base.worker.save

import java.util.concurrent.Future

trait SaveParsedProvider {
  def saveResults(parsedData: Map[String, String]): Future[Unit]
}
