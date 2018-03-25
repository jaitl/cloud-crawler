package com.github.jaitl.crawler.base.worker.save

import com.github.jaitl.crawler.base.worker.parser.ParseResult

import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String) extends SaveRawProvider {
  override def save(parseResult: ParseResult): Future[Unit] = ???
}
