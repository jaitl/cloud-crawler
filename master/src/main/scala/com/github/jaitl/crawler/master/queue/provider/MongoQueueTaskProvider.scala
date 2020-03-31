package com.github.jaitl.crawler.master.queue.provider

import java.time.Instant

import com.github.jaitl.crawler.master.client.task.Task
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.DeleteOneModel
import org.mongodb.scala.model.UpdateOneModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoQueueTaskProvider(
  connectionString: String,
  dbName: String,
  collectionName: String
) extends QueueTaskProvider {

  import org.mongodb.scala._ // scalastyle:ignore
  import org.mongodb.scala.bson.codecs.Macros._ // scalastyle:ignore
  import org.mongodb.scala.model.Filters._ // scalastyle:ignore
  import org.mongodb.scala.model.Updates._ // scalastyle:ignore

  private val codecRegistry = fromRegistries(fromProviders(classOf[MongoQueueTaskEntity]), DEFAULT_CODEC_REGISTRY)

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  private val collection: MongoCollection[MongoQueueTaskEntity] = database.getCollection(collectionName)

  override def pullBatch(taskType: String, size: Int): Future[Seq[Task]] =
    collection
      .find(and(equal("taskType", taskType), equal("taskStatus", TaskStatus.taskWait)))
      .limit(size)
      .map(
        entity =>
          Task(
            id = entity._id.toString,
            taskType = entity.taskType,
            taskData = entity.taskData,
            attempt = entity.attempt,
            lastUpdateMillis = entity.lastUpdate,
            skipped = false
        ))
      .toFuture()

  override def pullAndUpdateStatus(taskType: String, size: Int, taskStatus: String): Future[Seq[Task]] = {
    val transactionOptions = TransactionOptions
      .builder()
      .readConcern(ReadConcern.SNAPSHOT)
      .writeConcern(WriteConcern.MAJORITY)
      .build()

    val res = for {
      sess <- mongoClient.startSession()
      _ = sess.startTransaction(transactionOptions)
      data <- collection
        .find(sess, and(equal("taskType", taskType), equal("taskStatus", TaskStatus.taskWait)))
        .limit(size)
        .collect()
      updates = data.map(
        row =>
          UpdateOneModel(
            equal("_id", row._id),
            combine(set("taskStatus", taskStatus), set("lastUpdate", System.currentTimeMillis()))))
      _ <- collection.bulkWrite(sess, updates)
      _ = sess.commitTransaction()
      _ = sess.close()
    } yield data

    res
      .map(
        _.map(entity =>
          Task(
            id = entity._id.toString,
            taskType = entity.taskType,
            taskData = entity.taskData,
            attempt = entity.attempt,
            lastUpdateMillis = entity.lastUpdate
        )))
      .toFuture()
      .map(_.flatten)
  }

  override def pushTasks(taskData: Map[String, Seq[String]]): Future[Unit] = {
    val newTasks = taskData.flatMap {
      case (taskType, dataSeq) =>
        dataSeq.map(data => MongoQueueTaskEntity(new ObjectId(), taskType, data, TaskStatus.taskWait, 0, None))
    }.toSeq

    collection.insertMany(newTasks).toFuture().map(_ => ())
  }

  override def updateTasksStatus(ids: Seq[String], taskStatus: String): Future[Unit] = {
    val updates = ids.map { id =>
      UpdateOneModel(
        equal("_id", new ObjectId(id)),
        combine(set("taskStatus", taskStatus), set("lastUpdate", System.currentTimeMillis()))
      )
    }
    collection.bulkWrite(updates).toFuture().map(_ => ())
  }

  override def updateTasksStatusFromTo(time: Instant, fromStatus: String, toStatus: String): Future[Long] =
    collection
      .updateMany(
        and(equal("taskStatus", fromStatus), lte("lastUpdate", time.toEpochMilli)),
        set("taskStatus", toStatus)
      )
      .toFuture()
      .map(_.getModifiedCount)

  override def updateTasksStatusAndIncAttempt(ids: Seq[String], taskStatus: String): Future[Unit] = {
    val updates = ids.map { id =>
      UpdateOneModel(
        equal("_id", new ObjectId(id)),
        combine(
          set("taskStatus", taskStatus),
          inc("attempt", 1),
          set("lastUpdate", System.currentTimeMillis())
        )
      )
    }
    collection.bulkWrite(updates).toFuture().map(_ => ())
  }

  override def dropTasks(ids: Seq[String]): Future[Unit] = {
    val deletes = ids.map { id =>
      DeleteOneModel(equal("_id", new ObjectId(id)))
    }
    collection.bulkWrite(deletes).toFuture().map(_ => ())
  }

  override def getByIds(ids: Seq[String]): Future[Seq[Task]] = {
    val objIds = ids.map(id => new ObjectId(id))
    collection
      .find(in("_id", objIds: _*))
      .map(
        entity =>
          Task(
            id = entity._id.toString,
            taskType = entity.taskType,
            taskData = entity.taskData,
            attempt = entity.attempt,
            lastUpdateMillis = entity.lastUpdate,
            skipped = false
        ))
      .toFuture()
  }

  case class MongoQueueTaskEntity(
    _id: ObjectId,
    taskType: String,
    taskData: String,
    taskStatus: String,
    attempt: Int,
    lastUpdate: Option[Long]
  )
}
