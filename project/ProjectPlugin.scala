import de.heikoseeberger.sbtheader.{FileType, HeaderPlugin}
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import sbt._
import sbt.Keys._
import scoverage.ScoverageKeys.coverageMinimum

/**
  * Common project settings.
  */
object ProjectPlugin extends AutoPlugin {

  /** @see [[sbt.AutoPlugin]] */
  override val buildSettings = Seq(
    name := "opentracing-xray",
    organization := "com.abdulradi",
    
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings"),
    scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")
  )

  /** @see [[sbt.AutoPlugin]] */
  override val projectSettings = Seq(
    coverageMinimum := 100,

    publishArtifact in makePom := true,
    autoAPIMappings in Global := true,

    organizationName := "Tamer Abdulradi",
    startYear := Some(2017),
    licenses :=
      Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
  ) ++ addCommandAlias(
    "validate",
    ";reload plugins; sbt:scalafmt::test; scalafmt::test; reload return; " +
      "sbt:scalafmt::test; scalafmt::test; test:scalafmt::test; " +
      "headerCheck; test:headerCheck"
  )

}

object ProjectPluginKeys {
  // NOTE: anything in here is automatically visible in build.sbt
  /**
    * Implicitly add extra methods to in scope Projects
    *
    * @param p project that Play application setting should be applied to
    */
  implicit final class ExtraOps(val p: Project) extends AnyVal {
    def disablePublish(): Project =
      p.settings(Seq(
        publish := {},
        publishLocal := {},
        publishArtifact := false
      ))

    def enableProjectPlugin(): Project = 
      p.enablePlugins(ProjectPlugin)
  }
}
