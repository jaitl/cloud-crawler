package com.github.jaitl.cloud.base.creator

import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props

class PropsActorCreator(actorName: String, props: Props) extends ActorCreator {
  def create(factory: ActorRefFactory): ActorRef = factory.actorOf(props, actorName)
}
