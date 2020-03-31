package com.github.jaitl.crawler.worker.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.actor.Stash
import akka.pattern.pipe
import com.github.jaitl.crawler.master.client.task.Task
import com.github.jaitl.crawler.worker.client.QueueClient
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.creator.ThreeArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.BannedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailureSaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.ParsingFailedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SkippedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.crawler.worker.parser.ParseResult
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipeline
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.Scheduler

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success

class SaveCrawlResultController[T](
  pipeline: Pipeline[T],
  configPipeline: ConfigurablePipeline,
  queueClient: QueueClient,
  tasksBatchController: ActorRef,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig)
    extends Actor
    with ActorLogging
    with Stash {
  implicit private val executionContext: ExecutionContext = context.dispatcher

  var successTasks: mutable.Seq[SuccessCrawledTask] =
    mutable.ArraySeq.empty[SuccessCrawledTask]
  var failedTasks: mutable.Seq[FailedTask] = mutable.ArraySeq.empty[FailedTask]
  var skippedTasks: mutable.Seq[SkippedTask] =
    mutable.ArraySeq.empty[SkippedTask]
  var bannedTasks: mutable.Seq[BannedTask] = mutable.ArraySeq.empty[BannedTask]
  var parsingFailedTask: mutable.Seq[ParsingFailedTask] = mutable.ArraySeq.empty[ParsingFailedTask]

  override def preStart(): Unit = {
    super.preStart()

    saveScheduler.schedule(config.saveInterval, self, SaveResults)
  }

  override def receive: Receive = addResultHandler.orElse(waitSave)

  private def addResultHandler: Receive = {
    case AddResults(result) =>
      result match {
        case a @ SuccessCrawledTask(_, _, _) =>
          successTasks = successTasks :+ a
          sender() ! SuccessAddedResults

        case a @ FailedTask(_, _) =>
          failedTasks = failedTasks :+ a
          sender() ! SuccessAddedResults

        case a @ SkippedTask(_, _) =>
          skippedTasks = skippedTasks :+ a
          sender() ! SuccessAddedResults

        case a @ BannedTask(_, _) =>
          bannedTasks = bannedTasks :+ a
          sender() ! SuccessAddedResults

        case a @ ParsingFailedTask(_, _) =>
          parsingFailedTask = parsingFailedTask :+ a
          sender() ! SuccessAddedResults
      }
  }

  private def waitSave: Receive = {
    case SaveResults
        if successTasks.isEmpty && failedTasks.isEmpty && skippedTasks.isEmpty
          && parsingFailedTask.isEmpty && bannedTasks.isEmpty =>
      tasksBatchController ! SuccessSavedResults

    case SaveResults =>
      context.become(saveResultHandler)

      val parserResults =
        successTasks.flatMap(_.parseResult).map(_.parsedData.asInstanceOf[T])
      val rawResult = successTasks.map(r => (r.task, r.crawlResult))

      val saveFuture: Future[SaveResults] = for {
        _ <- (parserResults.nonEmpty, pipeline.saveParsedProvider) match {
          case (true, Some(provider)) => provider.saveResults(parserResults.toSeq)
          case _ => Future.successful(())
        }
        _ <- pipeline.saveRawProvider match {
          case Some(provider) => provider.save(rawResult.toSeq)
          case None => Future.successful(())
        }
      } yield SuccessSavedResults

      val recoveredSaveFuture = saveFuture.recover {
        case ex: Exception => FailureSaveResults(ex)
      }

      recoveredSaveFuture.pipeTo(self)
  }

  // scalastyle:off method.length
  private def saveResultHandler: Receive = {
    case success @ SuccessSavedResults =>
      context.unbecome()
      unstashAll()

      val successIds = successTasks.map(_.task.id).toSeq
      val failureIds = failedTasks.map(_.task.id).toSeq
      val skippedIds = skippedTasks.map(_.task.id).toSeq
      val bannedIds = bannedTasks.map(_.task.id).toSeq
      val parsingFailedTaskIds = parsingFailedTask.map(_.task.id).toSeq
      val newCrawlTasks = successTasks.flatMap(
        _.parseResult.map(_.newCrawlTasks).getOrElse(Seq.empty)
      )
      val newTasks = newCrawlTasks
        .groupBy(_.taskType)
        .map {
          case (taskType, vals) =>
            val newTasks = vals.flatMap(_.tasks).distinct.toSeq
            (taskType, newTasks)
        }

      val requestId = UUID.randomUUID()

      queueClient
        .putProcessResult(
          requestId = requestId,
          successIds = successIds,
          failureIds = failureIds,
          skippedIds = skippedIds,
          parsingFailedTaskIds = parsingFailedTaskIds,
          bannedIds = bannedIds,
          newTasks = newTasks
        )
        .onComplete {
          case Success(_) => log.debug(s"Crawl result sent to master, requestId: $requestId")
          case Failure(ex) => log.error(ex, s"Failure during sent crawl result to master, requestId: $requestId")
        }

      successTasks = mutable.ArraySeq.empty[SuccessCrawledTask]
      failedTasks = mutable.ArraySeq.empty[FailedTask]
      skippedTasks = mutable.ArraySeq.empty[SkippedTask]
      parsingFailedTask = mutable.ArraySeq.empty[ParsingFailedTask]
      bannedTasks = mutable.ArraySeq.empty[BannedTask]

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
  case class SuccessCrawledTask(
    task: Task,
    crawlResult: CrawlResult,
    parseResult: Option[ParseResult[_]]
  ) extends CrawlTaskResult
  case class FailedTask(task: Task, t: Throwable) extends CrawlTaskResult
  case class SkippedTask(task: Task, t: Throwable) extends CrawlTaskResult
  case class ParsingFailedTask(task: Task, t: Throwable) extends CrawlTaskResult
  case class BannedTask(task: Task, t: Throwable) extends CrawlTaskResult

  case class SaveCrawlResultControllerConfig(saveInterval: FiniteDuration)

  def props(
    pipeline: Pipeline[_],
    configPipeline: ConfigurablePipeline,
    queueClient: QueueClient,
    tasksBatchController: ActorRef,
    saveScheduler: Scheduler,
    config: SaveCrawlResultControllerConfig): Props =
    Props(
      new SaveCrawlResultController(
        pipeline = pipeline,
        configPipeline = configPipeline,
        queueClient = queueClient,
        tasksBatchController = tasksBatchController,
        saveScheduler = saveScheduler,
        config = config
      )
    )

  def name(): String = "saveCrawlResultController"
}

private[worker] class SaveCrawlResultControllerCreator(
  queueClient: QueueClient,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig
) extends ThreeArgumentActorCreator[Pipeline[_], ActorRef, ConfigurablePipeline] {
  override def create(
    factory: ActorRefFactory,
    firstArg: Pipeline[_],
    secondArg: ActorRef,
    thirdArg: ConfigurablePipeline): ActorRef =
    factory.actorOf(
      props = SaveCrawlResultController
        .props(
          pipeline = firstArg,
          configPipeline = thirdArg,
          queueClient = queueClient,
          tasksBatchController = secondArg,
          saveScheduler = saveScheduler,
          config = config
        )
        .withDispatcher("worker.blocking-io-dispatcher"),
      name = SaveCrawlResultController.name()
    )
}
