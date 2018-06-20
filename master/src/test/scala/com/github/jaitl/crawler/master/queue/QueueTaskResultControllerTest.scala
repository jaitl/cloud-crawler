package com.github.jaitl.crawler.master.queue

import java.util.UUID

import akka.actor.ActorRef
import com.github.jaitl.crawler.master.ActorTestSuite
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.AddNewTasks
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsFailed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsProcessed
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.models.worker.CommonActions.ActionSuccess

class QueueTaskResultControllerTest extends ActorTestSuite {

  trait SimpleMock {
    val queueProvider: QueueTaskProvider = mock[QueueTaskProvider]
    val queueTaskResultController: ActorRef = system.actorOf(QueueTaskResultController.props(queueProvider))

    val requestId: UUID = UUID.randomUUID()
    val taskType: String = "type1"
  }

  "QueueTaskResultController" should {
    "mark as processed" in new SimpleMock {
      val ids = Seq("id1", "id2")

      (queueProvider.dropTasks _).expects(ids).returning(futureSuccess)

      queueTaskResultController ! MarkAsProcessed(requestId, taskType, ids, self)

      expectMsg(ActionSuccess(requestId, taskType))
    }

    "mark as failed" in new SimpleMock {
      val ids = Seq("id1", "id2")

      (queueProvider.updateTasksStatus _).expects(ids, TaskStatus.taskWait).returning(futureSuccess)

      queueTaskResultController ! MarkAsFailed(requestId, taskType, ids, self)

      expectMsg(ActionSuccess(requestId, taskType))
    }

    "add new tasks" in new SimpleMock {
      val tasksData = Seq("data1", "data2")

      (queueProvider.pushTasks _).expects(taskType, tasksData).returning(futureSuccess)

      queueTaskResultController ! AddNewTasks(requestId, taskType, tasksData, self)

      expectMsg(ActionSuccess(requestId, taskType))
    }
  }
}
