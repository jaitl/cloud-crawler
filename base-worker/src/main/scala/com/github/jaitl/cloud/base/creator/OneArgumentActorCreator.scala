package com.github.jaitl.cloud.base.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait OneArgumentActorCreator[O] {
  def create(factory: ActorRefFactory, firstArg: O): ActorRef
}
