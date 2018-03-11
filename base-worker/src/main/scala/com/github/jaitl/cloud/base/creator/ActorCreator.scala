package com.github.jaitl.cloud.base.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait ActorCreator {
  def create(factory: ActorRefFactory): ActorRef
}
