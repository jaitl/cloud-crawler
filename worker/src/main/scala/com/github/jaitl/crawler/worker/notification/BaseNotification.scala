package com.github.jaitl.crawler.worker.notification

trait BaseNotification {
  def sendNotification(message: String, data: String): Unit
}
