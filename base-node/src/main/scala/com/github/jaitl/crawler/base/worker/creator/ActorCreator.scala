package com.github.jaitl.crawler.base.worker.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait ActorCreator {
  def create(factory: ActorRefFactory): ActorRef
}
