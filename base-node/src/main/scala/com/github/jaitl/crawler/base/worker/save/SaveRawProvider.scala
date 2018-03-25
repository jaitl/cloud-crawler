package com.github.jaitl.crawler.base.worker.save

import com.github.jaitl.crawler.base.worker.parser.ParseResult

import scala.concurrent.Future

trait SaveRawProvider {
  def save(parseResult: ParseResult): Future[Unit]
}
