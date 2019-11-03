package com.github.jaitl.crawler.master.config.provider

import java.time.Instant

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.worker.ProjectConfiguration
import com.github.jaitl.crawler.models.worker.CrawlerProxy
import com.github.jaitl.crawler.models.worker.CrawlerTor
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.types.ObjectId
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MongoConfigurationProvider(
  connectionString: String,
  dbName: String,
  configurationCollectionName: String,
  proxyCollectionName: String,
  torCollectionName: String,
) extends CrawlerConfigurationProvider {

  import org.mongodb.scala.bson.codecs.Macros._ // scalastyle:ignore
  import org.mongodb.scala.model.Filters._ // scalastyle:ignore
  import org.mongodb.scala.model.Updates._ // scalastyle:ignore

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val codecRegistry = fromRegistries(
    fromProviders(classOf[MongoProjectConfiguration], classOf[MongoCrawlerProxy], classOf[CrawlerTor]),
    DEFAULT_CODEC_REGISTRY)
  private val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  private val crawlerProjectConfiguration: MongoCollection[MongoProjectConfiguration] =
    database.getCollection(configurationCollectionName)
  private val crawlerProxies: MongoCollection[MongoCrawlerProxy] =
    database.getCollection(proxyCollectionName)
  private val crawlerTors: MongoCollection[CrawlerTor] =
    database.getCollection(torCollectionName)

  override def getCrawlerProjectConfiguration(taskType: String): Future[Seq[ProjectConfiguration]] =
    crawlerProjectConfiguration
      .find(equal("workerTaskType", taskType))
      .map(entity =>
        ProjectConfiguration(
          entity._id.toString,
          entity.workerExecuteInterval,
          entity.workerFilePath,
          entity.workerBatchSize,
          entity.workerBaseUrl,
          entity.workerTaskType,
          entity.workerBatchSize,
          entity.workerResource
      ))
      .toFuture()

  override def getCrawlerProxyConfiguration(taskType: String): Future[Seq[CrawlerProxy]] =
    crawlerProxies
      .find(equal("workerTaskType", taskType))
      .map(entity =>
        CrawlerProxy(
          entity._id.toString,
          entity.workerProxyHost,
          entity.workerProxyPort,
          entity.workerProxyTimeoutUp,
          entity.workerProxyTimeoutDown,
          entity.workerParallel,
          entity.workerProxyLogin,
          entity.workerProxyPassword,
          entity.workerTaskType
      ))
      .toFuture()

  override def getCrawlerTorConfiguration(taskType: String): Future[Seq[CrawlerTor]] =
    crawlerTors
      .find(equal("workerTaskType", taskType))
      .toFuture()
}
case class MongoProjectConfiguration(
  _id: ObjectId,
  workerExecuteInterval: String,
  workerFilePath: String,
  workerBatchSize: Int,
  workerBaseUrl: String,
  workerTaskType: String,
  workerParallelBatches: Int,
  workerResource: String
)

case class MongoCrawlerProxy(
  _id: ObjectId,
  workerProxyHost: String,
  workerProxyPort: Int,
  workerProxyTimeoutUp: String,
  workerProxyTimeoutDown: String,
  workerParallel: Int,
  workerProxyLogin: String,
  workerProxyPassword: String,
  workerTaskType: String
)
