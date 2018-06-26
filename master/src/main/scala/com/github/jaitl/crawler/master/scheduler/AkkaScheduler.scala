package com.github.jaitl.crawler.master.scheduler
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Cancellable

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration

class AkkaScheduler(system: ActorSystem) extends Scheduler {
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private var cancellable: Cancellable = _

  override def schedule(interval: FiniteDuration, receiver: ActorRef, message:  Any): Unit = {
    cancellable = system.scheduler.schedule(interval, interval, receiver, message)
  }
}
