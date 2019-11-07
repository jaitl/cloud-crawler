package com.github.jaitl.crawler.worker.notification

import akka.actor.Actor
import akka.actor.Props

import com.github.jaitl.crawler.worker.notification.NotificationExecutor.SendNotification
import com.github.jaitl.crawler.worker.pipeline.Pipeline

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

private class NotificationExecutor extends Actor {
  implicit private val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case SendNotification(message, data, pipeline) =>
      Future(pipeline.notifier.map(notifier => notifier.sendNotification(message, data)))
  }
}
private[worker] object NotificationExecutor {

  case class SendNotification(
    message: String,
    data: String,
    pipeline: Pipeline[_]
  )

  def props(): Props = Props(new NotificationExecutor)

  def name(): String = "NotificationExecutor"
}
