import sbt.Keys._
import sbt._
import sbtprotoc.ProtocPlugin.autoImport._

object ProtobufSettings {
  lazy val protoSettings = Seq(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )
}
