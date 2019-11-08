package com.github.jaitl.crawler.worker.notification

class DummyNotification extends BaseNotification {
  override def sendNotification(message: String, data: String): Unit = ???
}
