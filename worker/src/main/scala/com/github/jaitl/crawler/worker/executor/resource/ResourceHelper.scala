package com.github.jaitl.crawler.worker.executor.resource

import java.io.IOException

private[worker] object ResourceHelper {
  def isResourceFailed(t: Throwable): Boolean = t match {
    case _: IOException => true
    case _ => false
  }
}
