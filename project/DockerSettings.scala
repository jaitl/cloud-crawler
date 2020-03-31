import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerBaseImage
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerVersion
import com.typesafe.sbt.packager.docker.DockerVersion
import sbt.Keys.version
import sbt.Setting

object DockerSettings {
  lazy val dockerSettings: Seq[Setting[_]] = Seq(
    dockerPermissionStrategy := DockerPermissionStrategy.Run,
    dockerVersion := Some(DockerVersion(18, 9, 0, Some("ce"))),
    version in Docker := "latest",
    dockerBaseImage := "openjdk:11-jre-slim"
  )
}
