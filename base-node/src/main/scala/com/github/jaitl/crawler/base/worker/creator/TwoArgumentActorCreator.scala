package com.github.jaitl.crawler.base.worker.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait TwoArgumentActorCreator[O, T] {
  def create(factory: ActorRefFactory, firstArg: O, secondArg: T): ActorRef
}
