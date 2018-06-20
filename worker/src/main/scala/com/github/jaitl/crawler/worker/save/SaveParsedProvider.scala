package com.github.jaitl.crawler.worker.save

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


trait SaveParsedProvider[T] {
  def saveResults[D](parsedData: Seq[D])(implicit executionContext: ExecutionContext): Future[Unit]
}
