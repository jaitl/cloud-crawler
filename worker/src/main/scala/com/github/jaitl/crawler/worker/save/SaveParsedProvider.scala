package com.github.jaitl.crawler.worker.save

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


trait SaveParsedProvider[T] {
  def saveResults(parsedData: Seq[T])(implicit executionContext: ExecutionContext): Future[Unit]
}
