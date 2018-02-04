package com.github.jaitl.cloud.master

import akka.actor.Actor
import akka.actor.ActorLogging
import com.github.jaitl.cloud.master.TaskManager.DelegateTask

class TaskManager extends Actor with ActorLogging {
  override def receive: Receive = {
    case DelegateTask() =>

  }
}

object TaskManager {
  trait TaskManagerCommand

  case class DelegateTask()
}
