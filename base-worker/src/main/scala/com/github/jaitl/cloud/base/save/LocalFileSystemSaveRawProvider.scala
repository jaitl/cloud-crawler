package com.github.jaitl.cloud.base.save

import com.github.jaitl.cloud.base.parser.ParseResult

import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String) extends SaveRawProvider {
  override def save(parseResult: ParseResult): Future[Unit] = ???
}
