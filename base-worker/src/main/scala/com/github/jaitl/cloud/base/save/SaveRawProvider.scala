package com.github.jaitl.cloud.base.save

import com.github.jaitl.cloud.base.parser.ParseResult

import scala.concurrent.Future

trait SaveRawProvider {
  def save(parseResult: ParseResult): Future[Unit]
}
