package com.github.jaitl.crawler.master.config.provider

import com.github.jaitl.crawler.models.worker.ProjectConfiguration
import com.github.jaitl.crawler.models.worker.CrawlerProxy
import com.github.jaitl.crawler.models.worker.CrawlerTor
import com.mongodb.client.model.FindOneAndUpdateOptions
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.types.ObjectId
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.model.Sorts

import scala.concurrent.Future

class MongoConfigurationProvider(
  connectionString: String,
  dbName: String,
  configurationCollectionName: String,
  proxyCollectionName: String,
  torCollectionName: String
) extends CrawlerConfigurationProvider {

  import org.mongodb.scala.bson.codecs.Macros._ // scalastyle:ignore
  import org.mongodb.scala.model.Filters._ // scalastyle:ignore
  import org.mongodb.scala.model.Updates._ // scalastyle:ignore

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val codecRegistry = fromRegistries(
    fromProviders(classOf[MongoProjectConfiguration], classOf[MongoCrawlerProxy], classOf[MongoCrawlerTor]),
    DEFAULT_CODEC_REGISTRY)
  private val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  private val crawlerProjectConfiguration: MongoCollection[MongoProjectConfiguration] =
    database.getCollection(configurationCollectionName)
  private val crawlerProxies: MongoCollection[MongoCrawlerProxy] =
    database.getCollection(proxyCollectionName)
  private val crawlerTors: MongoCollection[MongoCrawlerTor] =
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
          entity.workerResource,
          entity.workerNotification
      ))
      .toFuture()

  override def getCrawlerProxyConfiguration(taskType: String): Future[Seq[CrawlerProxy]] =
    crawlerProxies
      .findOneAndUpdate(
        equal("workerTaskType", taskType),
        inc("usedCount", 1),
        new FindOneAndUpdateOptions().sort(Sorts.ascending("usedCount"))
      )
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
      .findOneAndUpdate(
        equal("workerTaskType", taskType),
        inc("usedCount", 1),
        new FindOneAndUpdateOptions().sort(Sorts.ascending("usedCount"))
      )
      .map(entity =>
        CrawlerTor(
          entity._id.toString,
          entity.workerTorHost,
          entity.workerTorLimit,
          entity.workerTorPort,
          entity.workerTorControlPort,
          entity.workerTorPassword,
          entity.workerTorTimeoutUp,
          entity.workerTorTimeoutDown,
          entity.workerTaskType
        ))
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
  workerResource: String,
  workerNotification: Boolean
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
  workerTaskType: List[String]
)
case class MongoCrawlerTor(
  _id: ObjectId,
  workerTorHost: String,
  workerTorLimit: Int,
  workerTorPort: Int,
  workerTorControlPort: Int,
  workerTorPassword: String,
  workerTorTimeoutUp: String,
  workerTorTimeoutDown: String,
  workerTaskType: String
)
