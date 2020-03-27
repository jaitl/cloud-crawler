import BuildSettings._
import Dependencies._
import DockerSettings._
import ProtobufSettings._

val projectVersion = sys.env.getOrElse("RELEASE_VERSION", "SNAPSHOT")

lazy val root = (project in file("."))
  .aggregate(master, `master-client`, worker, `simple-worker`, `integration-tests`)
  .settings(commonSettings)
  .settings(
    name := "cloud-crawler",
    version := projectVersion,
    skip in publish := true
  )

lazy val `master-client` = (project in file("master-client"))
  .settings(name := "master-client", version := projectVersion)
  .settings(commonSettings)
  .settings(protoSettings)
  .settings(libraryDependencies ++= gRpc.list)

lazy val master = (project in file("master"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "master",
    version := projectVersion,
    mainClass in run := Some("com.github.jaitl.crawler.master.MasterApp"),
    skip in publish := true
  )
  .settings(commonSettings)
  .settings(dockerSettings)
  .dependsOn(`master-client`)
  .settings(
    libraryDependencies ++= Seq(mongoScalaDriver, ficus) ++ Akka.list ++ Logging.list,
    libraryDependencies ++= Seq(scalaTest, scalamock)
  )

lazy val worker = (project in file("worker"))
  .settings(name := "worker", version := projectVersion)
  .settings(commonSettings)
  .dependsOn(`master-client`)
  .settings(
    libraryDependencies ++= Seq(mongoScalaDriver, ficus, asyncHttpClient, awsSdk, json4s, jtorctl) ++ Akka.list ++ Logging.list ++ Elasticsearch.list,
    libraryDependencies ++= Seq(scalaTest, scalamock)
  )

lazy val `simple-worker` = (project in file("simple-worker"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "simple-worker",
    version := projectVersion,
    mainClass in run := Some("com.github.jaitl.cloud.simple.worker.App"),
    skip in publish := true
  )
  .settings(commonSettings)
  .settings(dockerSettings)
  .dependsOn(worker)
  .settings(
    libraryDependencies += jsoup,
    libraryDependencies += scalaTest
  )

lazy val `integration-tests` = (project in file("integration-tests"))
  .configs(IntegrationTest)
  .settings(
    name := "integration-tests",
    version := projectVersion,
    skip in publish := true
  )
  .settings(commonSettings)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(scalaTestIt, testContainersIt)
  )
