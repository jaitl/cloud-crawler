package com.github.jaitl.crawler.master.queue.provider

object TaskStatus {
  val taskWait: String = "taskWait"
  val taskParsingFailed: String = "taskParsingFailed"
  val taskInProgress: String = "taskInProgress"
  val taskFailed: String = "taskFailed"
  val taskSkipped: String = "taskSkipped"
  val taskFinished: String = "taskFinished"
}
