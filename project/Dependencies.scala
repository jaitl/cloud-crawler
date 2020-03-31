import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
  lazy val scalamock = "org.scalamock" %% "scalamock" % "4.4.0" % Test

  lazy val scalaTestIt = "org.scalatest" %% "scalatest" % "3.1.1" % IntegrationTest
  lazy val testContainersIt = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.36.1" % IntegrationTest

  lazy val ficus = "com.iheart" %% "ficus" % "1.4.7"
  
  lazy val mongoScalaDriver = "org.mongodb.scala" %% "mongo-scala-driver" % "4.0.1"
  lazy val asyncHttpClient = "org.asynchttpclient" % "async-http-client" % "2.11.0"
  lazy val awsSdk = "com.amazonaws" % "aws-java-sdk" % "1.11.754"
  lazy val json4s = "org.json4s" %% "json4s-jackson" % "3.6.7"
  lazy val jtorctl = "com.github.avchu" % "jtorctl" % "v0.3.3"
  lazy val jsoup = "org.jsoup" % "jsoup" % "1.13.1"

  object Logging {
    lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

    lazy val list: Seq[ModuleID] = Seq(logback, scalaLogging)
  }

  object Akka {
    val version = "2.6.4"

    lazy val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
    lazy val actor = "com.typesafe.akka" %% "akka-actor" % version
    lazy val testkit = "com.typesafe.akka" %% "akka-testkit" % version % Test

    lazy val list: Seq[ModuleID] = Seq(actor, slf4j, testkit)
  }

  object Elasticsearch {
    val version = "6.7.4"

    lazy val elastic4sCore = "com.sksamuel.elastic4s" %% "elastic4s-core" % version
    lazy val elastic4sHttp = "com.sksamuel.elastic4s" %% "elastic4s-http" % version

    lazy val list: Seq[ModuleID] = Seq(elastic4sCore, elastic4sHttp)
  }

  object gRpc {
    lazy val list = Seq(
      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  }
}
