package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.cloud.base.PiplineWorker.Task
import com.github.jaitl.cloud.base.PiplineWorker.TaskSuccessResult

class PiplineWorker(piplineScheduler: PiplineScheduler, taskManager: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Task(id, master) =>
      log.debug(s"new task, id: $id")
      master ! TaskSuccessResult(id)
      context.stop(self)
  }
}

object PiplineWorker {
  trait PiplineCommand

  case class RequestTask() extends PiplineCommand
  case class Task(id: UUID, master: ActorRef) extends PiplineCommand
  case class TaskSuccessResult(id: UUID) extends PiplineCommand
  case class TaskFailureResult(id: UUID, throwable: Throwable) extends PiplineCommand

  def props(piplineScheduler: PiplineScheduler, taskManager: ActorRef): Props =
    Props(new PiplineWorker(piplineScheduler, taskManager))

  def name(id: UUID): String = s"PiplineWorker-${id.toString}"
}
