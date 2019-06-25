import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._
import bintray.BintrayKeys._

object BuildSettings {
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    scalaVersion := "2.12.8",
    organization := "com.github.jaitl",
    organizationName := "JaitlApp",
    resolvers += "jitpack" at "https://jitpack.io",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayRepository := "cloud-crawler",
    scalacOptions := Seq(
      "-encoding",
      "UTF-8", // source files are in UTF-8
      "-deprecation", // warn about use of deprecated APIs
      "-unchecked", // warn about unchecked type parameters
      "-feature", // warn about misused language features
      "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
      "-Xlint", // enable handy linter warnings
      // "-Xfatal-warnings", // turn compiler warnings into errors
      "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
    ),
    coverageExcludedPackages := Seq(
      "<empty>",
      ".*WorkerApp.*",
      ".*MasterApp.*",
      ".*MongoQueueTaskProvider.*",
      ".*QueueTaskProviderFactory.*",
      ".*\\.simple\\.worker\\..*",
      ".*\\.crawler\\.models\\..*"
    ).mkString(";")
  )
}
