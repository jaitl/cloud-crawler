package com.github.jaitl.crawler.worker.executor.resource

import java.io.IOException

import com.github.jaitl.crawler.worker.exception.PageNotFoundException

private[worker] object ResourceHelper {
  def isResourceSkipped(t: Throwable): Boolean = t match {
    case _: PageNotFoundException => true
    case _ => false
  }

  def isResourceFailed(t: Throwable): Boolean = t match {
    case _: IOException => true
    case _ => false
  }
}
