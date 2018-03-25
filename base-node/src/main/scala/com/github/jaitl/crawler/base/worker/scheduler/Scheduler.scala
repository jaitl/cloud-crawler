package com.github.jaitl.crawler.base.worker.scheduler

import akka.actor.ActorRef

import scala.concurrent.duration.FiniteDuration

trait Scheduler {
  def schedule(interval: FiniteDuration, receiver: ActorRef, message:  Any): Unit
  def scheduleOnce(delay: FiniteDuration, receiver: ActorRef, message:  Any): Unit
  def reSchedule(interval: FiniteDuration, receiver: ActorRef, message:  Any): Unit
  def reScheduleOnce(delay: FiniteDuration, receiver: ActorRef, message:  Any): Unit
}
