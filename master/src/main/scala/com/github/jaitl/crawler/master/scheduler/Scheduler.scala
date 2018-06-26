package com.github.jaitl.crawler.master.scheduler

import akka.actor.ActorRef

import scala.concurrent.duration.FiniteDuration

trait Scheduler {
  def schedule(interval: FiniteDuration, receiver: ActorRef, message:  Any): Unit
}
