package com.github.jaitl.crawler.worker.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait ThreeArgumentActorCreator[O, T, B] {
  def create(factory: ActorRefFactory, firstArg: O, secondArg: T, thirdArg: B): ActorRef
}
