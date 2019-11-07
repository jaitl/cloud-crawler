package com.github.jaitl.crawler.worker.email

trait BaseNotification {
  def sendNotification(message: String, data: String): Unit
}
