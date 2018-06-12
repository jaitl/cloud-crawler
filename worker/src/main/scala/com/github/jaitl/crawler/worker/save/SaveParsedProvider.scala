package com.github.jaitl.crawler.worker.save

import java.util.concurrent.Future

trait SaveParsedProvider[T] {
  def saveResults(parsedData: Seq[T]): Future[Unit]
}
