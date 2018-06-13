package com.github.jaitl.crawler.worker.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait OneArgumentActorCreator[O] {
  def create(factory: ActorRefFactory, firstArg: O): ActorRef
}
