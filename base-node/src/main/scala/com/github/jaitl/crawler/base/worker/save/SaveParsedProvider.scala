package com.github.jaitl.crawler.base.worker.save

import java.util.concurrent.Future

import com.github.jaitl.crawler.base.worker.parser.ParsedData

trait SaveParsedProvider[T <: ParsedData] {
  def saveResults(parsedData: Seq[T]): Future[Unit]
}
