package com.github.jaitl.crawler.master.queue

import java.util.UUID

import akka.testkit.TestProbe
import com.github.jaitl.crawler.master.ActorTestSuite
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController.RequestTask
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.AddNewTasks
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsFailed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsProcessed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.ReturnToQueue
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestTasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.ReturnTasks
import com.github.jaitl.crawler.models.worker.WorkerManager.TaskTypeWithBatchSize
import com.github.jaitl.crawler.models.worker.WorkerManager.TasksBatchProcessResult

class QueueTaskBalancerTest extends ActorTestSuite {
  "QueueTaskBalancer" should {
    "send batch tasks request" in {
      val queueTaskQueueReqCtrl = TestProbe()
      val queueTaskQueueResCtrl = TestProbe()

      val queueTaskBalancer = system
        .actorOf(QueueTaskBalancer.props(queueTaskQueueReqCtrl.ref, queueTaskQueueResCtrl.ref))

      val taskTypes = TaskTypeWithBatchSize("type1", 10) :: Nil
      val request = RequestTasksBatch(UUID.randomUUID(), taskTypes)

      queueTaskBalancer ! request

      queueTaskQueueReqCtrl.expectMsg(RequestTask(request.requestId, "type1", 10, self))
    }

    "send batch tasks result" in {
      val queueTaskQueueReqCtrl = TestProbe()
      val queueTaskQueueResCtrl = TestProbe()
      val queueTaskBalancer = system
        .actorOf(QueueTaskBalancer.props(queueTaskQueueReqCtrl.ref, queueTaskQueueResCtrl.ref))

      val requestId = UUID.randomUUID()
      val taskType = "type1"
      val request = TasksBatchProcessResult(
        requestId = requestId,
        taskType = taskType,
        successIds = Seq("1", "2"),
        failureIds = Seq("3", "4"),
        newTasks = Map("type2" -> Seq("tt1", "tt2"))
      )

      queueTaskBalancer ! request

      queueTaskQueueResCtrl.expectMsg(MarkAsProcessed(requestId, taskType, Seq("1", "2"), self))
      queueTaskQueueResCtrl.expectMsg(MarkAsFailed(requestId, taskType, Seq("3", "4"), self))
      queueTaskQueueResCtrl.expectMsg(AddNewTasks(requestId, "type2", Seq("tt1", "tt2"), self))
    }

    "return tasks" in {
      val queueTaskQueueReqCtrl = TestProbe()
      val queueTaskQueueResCtrl = TestProbe()
      val queueTaskBalancer = system
        .actorOf(QueueTaskBalancer.props(queueTaskQueueReqCtrl.ref, queueTaskQueueResCtrl.ref))

      val requestId = UUID.randomUUID()
      val taskType = "type1"

      val request = ReturnTasks(
        requestId = requestId,
        taskType = taskType,
        ids = Seq("1", "2", "3")
      )

      queueTaskBalancer ! request

      queueTaskQueueResCtrl.expectMsg(ReturnToQueue(requestId, taskType, Seq("1", "2", "3"), self))
    }
  }
}
