package com.github.jaitl.crawler.worker.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait ActorCreator {
  def create(factory: ActorRefFactory): ActorRef
}
