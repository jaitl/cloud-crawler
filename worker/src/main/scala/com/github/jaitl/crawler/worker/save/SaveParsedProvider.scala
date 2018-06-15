package com.github.jaitl.crawler.worker.save

import scala.concurrent.Future


trait SaveParsedProvider[T] {
  def saveResults[T](parsedData: Seq[T]): Future[Unit]
}
