package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import com.github.jaitl.cloud.base.PiplineWorker.Task
import com.github.jaitl.cloud.base.PiplineWorker.TaskResult

class PiplineWorker extends Actor with ActorLogging {

  override def receive: Receive = {
    case Task(id, master) =>
      log.debug(s"new task, id: $id")
      master ! TaskResult(id)
      context.stop(self)
  }
}

object PiplineWorker {
  trait PiplineCommand

  case class Task(id: UUID, master: ActorRef) extends PiplineCommand
  case class TaskResult(id: UUID) extends PiplineCommand
}
