package com.github.jaitl.cloud.base

import akka.actor.ActorSystem

class SimplePiplineScheduler()(implicit system: ActorSystem) extends PiplineScheduler {
  override def scheduleOnce(): Unit = ???
}
