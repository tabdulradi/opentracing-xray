import ProjectPluginKeys._

import com.abdulradi.Dependencies._

scalaVersion in ThisBuild := "2.12.3"
crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.3")

lazy val `opentracing-xray` =
  (project in file("."))
    .enableProjectPlugin()
    .settings(
      libraryDependencies ++= Seq(
        Jaeger.core,
        Refined.core,
        circe
      )
    )

addCommandAlias(
  "fmt",
  ";headerCreate;test:headerCreate;sbt:scalafmt;scalafmt;test:scalafmt"
)

// while you're working, try putting "~wip" into your sbt console
// ...but be prepared to let IntelliJ force you to reload your source code!
addCommandAlias("wip", ";fmt;test:compile")
