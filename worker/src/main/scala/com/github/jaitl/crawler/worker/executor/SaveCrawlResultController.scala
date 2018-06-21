package com.github.jaitl.crawler.worker.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.actor.Stash
import akka.pattern.pipe
import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.worker.WorkerManager.TasksBatchProcessResult
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailureSaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.crawler.worker.parser.ParseResult
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.Scheduler

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SaveCrawlResultController[T](
  pipeline: Pipeline[T],
  queueTaskBalancer: ActorRef,
  tasksBatchController: ActorRef,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig
) extends Actor with ActorLogging with Stash {
  private implicit val executionContext: ExecutionContext = context.dispatcher

  var successTasks: mutable.Seq[SuccessCrawledTask] = mutable.ArraySeq.empty[SuccessCrawledTask]
  var failedTasks: mutable.Seq[FailedTask] = mutable.ArraySeq.empty[FailedTask]

  override def preStart(): Unit = {
    super.preStart()

    saveScheduler.schedule(config.saveInterval, self, SaveResults)
  }

  override def receive: Receive = addResultHandler orElse waitSave

  private def addResultHandler: Receive = {
    case AddResults(result) =>
      result match {
        case a @ SuccessCrawledTask(_, _, _) =>
          successTasks = successTasks :+ a
          sender() ! SuccessAddedResults

        case a @ FailedTask(_, _) =>
          failedTasks = failedTasks :+ a
          sender() ! SuccessAddedResults
      }
  }

  private def waitSave: Receive = {
    case SaveResults if successTasks.isEmpty && failedTasks.isEmpty =>
      tasksBatchController ! SuccessSavedResults

    case SaveResults =>
      context.become(saveResultHandler)

      val parserResults = successTasks.flatMap(_.parseResult).map(_.parsedData.asInstanceOf[T])
      val rawResult = successTasks.map(r => (r.task, r.crawlResult))

      val saveFuture: Future[SaveResults] = for {
        _ <- (parserResults.nonEmpty, pipeline.saveParsedProvider) match {
          case (true, Some(provider)) => provider.saveResults(parserResults)
          case _ => Future.successful(Unit)
        }
        _ <- pipeline.saveRawProvider match {
          case Some(provider) => provider.save(rawResult)
          case None => Future.successful(Unit)
        }
      } yield SuccessSavedResults

      val recoveredSaveFuture = saveFuture.recover {
        case ex: Exception => FailureSaveResults(ex)
      }

      recoveredSaveFuture pipeTo self
  }

  private def saveResultHandler: Receive = {
    case success @ SuccessSavedResults =>
      context.unbecome()
      unstashAll()

      val successIds = successTasks.map(_.task.id)
      val failureIds = failedTasks.map(_.task.id)
      val newCrawlTasks = successTasks.flatMap(_.parseResult.map(_.newCrawlTasks).getOrElse(Seq.empty))
      val newTasks = newCrawlTasks.groupBy(_.taskType)
        .map {
          case (taskType, vals) =>
            val newTasks = vals.flatMap(_.tasks).distinct
            (taskType, newTasks)
        }

      queueTaskBalancer ! TasksBatchProcessResult(
        requestId = UUID.randomUUID(),
        taskType = pipeline.taskType,
        successIds = successIds,
        failureIds = failureIds,
        newTasks = newTasks
      )

      successTasks = mutable.ArraySeq.empty[SuccessCrawledTask]
      failedTasks = mutable.ArraySeq.empty[FailedTask]

      tasksBatchController ! success

    case failure @ FailureSaveResults(t) =>
      log.error(t, "Error during save results")
      context.unbecome()
      unstashAll()

      tasksBatchController ! failure

    case _: Any => stash()
  }
}

object SaveCrawlResultController {

  trait SaveResults
  case object SaveResults extends SaveResults
  case object SuccessSavedResults extends SaveResults
  case class FailureSaveResults(t: Throwable) extends SaveResults

  case class AddResults(result: CrawlTaskResult)
  case object SuccessAddedResults

  trait CrawlTaskResult
  case class SuccessCrawledTask(task: Task, crawlResult: CrawlResult, parseResult: Option[ParseResult[_]]) extends CrawlTaskResult
  case class FailedTask(task: Task, t: Throwable) extends CrawlTaskResult

  case class SaveCrawlResultControllerConfig(saveInterval: FiniteDuration)

  def props(
    pipeline: Pipeline[_],
    queueTaskBalancer: ActorRef,
    tasksBatchController: ActorRef,
    saveScheduler: Scheduler,
    config: SaveCrawlResultControllerConfig
  ): Props = Props(new SaveCrawlResultController(
    pipeline = pipeline,
    queueTaskBalancer = queueTaskBalancer,
    tasksBatchController = tasksBatchController,
    saveScheduler = saveScheduler,
    config = config
  ))

  def name(): String = "saveCrawlResultController"
}

private[worker] class SaveCrawlResultControllerCreator(
  queueTaskBalancer: ActorRef,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig
) extends TwoArgumentActorCreator[Pipeline[_], ActorRef] {
  override def create(factory: ActorRefFactory, firstArg: Pipeline[_], secondArg: ActorRef): ActorRef = {
    factory.actorOf(
      props = SaveCrawlResultController.props(
        pipeline = firstArg,
        queueTaskBalancer = queueTaskBalancer,
        tasksBatchController = secondArg,
        saveScheduler = saveScheduler,
        config = config
      ).withDispatcher("worker.blocking-io-dispatcher"),
      name = SaveCrawlResultController.name()
    )
  }
}
