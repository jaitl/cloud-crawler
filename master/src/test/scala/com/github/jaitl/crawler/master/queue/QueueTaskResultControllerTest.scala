package com.github.jaitl.crawler.master.queue

import java.util.UUID

import akka.actor.ActorRef
import com.github.jaitl.crawler.master.ActorTestSuite
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.AddNewTasks
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsFailed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsProcessed
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.worker.CommonActions.ActionSuccess

import scala.concurrent.Future

class QueueTaskResultControllerTest extends ActorTestSuite {

  trait SimpleMock {
    val queueTaskConfig = QueueTaskConfig(3)

    val queueProvider: QueueTaskProvider = mock[QueueTaskProvider]
    val queueTaskResultController: ActorRef =
      system.actorOf(QueueTaskResultController.props(queueProvider, queueTaskConfig))

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
      val ids = Seq("id1", "id2", "id3", "id4")

      val tasks = Seq(
        Task("id1", taskType, "1", 0),
        Task("id2", taskType, "2", 1),
        Task("id3", taskType, "3", 2),
        Task("id4", taskType, "4", 2)
      )

      (queueProvider.getByIds _).expects(ids).returning(Future.successful(tasks))

      (queueProvider.updateTasksStatusAndIncAttempt _)
        .expects(Seq("id1", "id2"), TaskStatus.taskWait).returning(futureSuccess)

      (queueProvider.updateTasksStatusAndIncAttempt _)
        .expects(Seq("id3", "id4"), TaskStatus.taskFailed).returning(futureSuccess)

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
