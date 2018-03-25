package com.github.jaitl.crawler.base.master.queue

import java.util.UUID

import akka.testkit.TestProbe
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.RequestTasksBatch
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.TaskTypeWithBatchSize
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.TasksBatchProcessResult
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.AddNewTasks
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.MarkAsFailed
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.MarkAsProcessed
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.RequestTask
import com.github.jaitl.crawler.base.test.ActorTestSuite

class QueueTaskBalancerTest extends ActorTestSuite {
  test("send batch tasks request") {
    val queueTaskTypedManager = TestProbe()
    val queueTaskBalancer = system.actorOf(QueueTaskBalancer.props(queueTaskTypedManager.ref))

    val taskTypes = TaskTypeWithBatchSize("type1", 10) :: Nil
    val request = RequestTasksBatch(UUID.randomUUID(), taskTypes)

    queueTaskBalancer ! request

    queueTaskTypedManager.expectMsg(RequestTask(request.requestId, "type1", 10, self))
  }

  test("send batch tasks result") {
    val queueTaskTypedManager = TestProbe()
    val queueTaskBalancer = system.actorOf(QueueTaskBalancer.props(queueTaskTypedManager.ref))

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

    queueTaskTypedManager.expectMsg(MarkAsProcessed(requestId, taskType, Seq("1", "2"), self))
    queueTaskTypedManager.expectMsg(MarkAsFailed(requestId, taskType, Seq("3", "4"), self))
    queueTaskTypedManager.expectMsg(AddNewTasks(requestId, "type2", Seq("tt1", "tt2"), self))
  }
}
