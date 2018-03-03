package com.github.jaitl.cloud.base.save

import java.util.concurrent.Future

trait SaveParsedProvider {
  def saveResults(parsedData: Map[String, String]): Future[Unit]
}
