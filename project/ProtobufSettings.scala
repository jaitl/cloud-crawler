import sbt.Keys._
import sbt._
import sbtprotoc.ProtocPlugin.autoImport._

import scala.sys.process.Process
object ProtobufSettings {
  lazy val protoSettings = Seq(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    PB.runProtoc in Compile := (args => Process("/usr/local/bin/protoc", args)!)
  )
}
