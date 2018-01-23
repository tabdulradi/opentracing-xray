import ProjectPluginKeys._

import com.abdulradi.Dependencies._

scalaVersion in ThisBuild := "2.12.3"
crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.3")

lazy val `opentracing-xray-core` =
  (project in file("modules/core"))
    .enableProjectPlugin()
    .settings(
      libraryDependencies ++= Seq(
        atto,
        Refined.core,
        circe,
        circeRefined
      ) 
    )

lazy val `opentracing-xray-jaeger` =
  (project in file("modules/jaeger"))
    .enableProjectPlugin()
    .dependsOn(`opentracing-xray-core`)
    .settings(
      libraryDependencies ++= Seq(
        Jaeger.core,
        scalaTest % Test,
        circeParser % Test
      )
    )

lazy val `opentracing-xray-zipkin` =
  (project in file("modules/zipkin"))
    .enableProjectPlugin()
    .dependsOn(`opentracing-xray-core`)
    .settings(
      libraryDependencies ++= Seq(
        "io.zipkin.brave" % "brave" % "4.9.0",
        "io.zipkin.brave" % "brave-propagation-aws" % "4.9.0",
        "io.opentracing.brave" % "brave-opentracing" % "0.22.1"
//        "io.zipkin.aws" % "zipkin-reporter-xray-udp" % "0.8.5-SNAPSHOT"
      )
    )

/*
lazy val `opentracing-xray-fat` = project
  .enablePlugins(AssemblyPlugin)
  .dependsOn(`opentracing-xray`)
  .settings(
    assemblyShadeRules in assembly := Seq(
      // TODO: Drop Jaeger!
      ShadeRule.rename("cats.**" -> "com.abdulradi.opentracing.xray.shaded_libs.cats.@1").inAll,
      ShadeRule.rename("io.circe.**" -> "com.abdulradi.opentracing.xray.shaded_libs.circe.@1").inAll,
      ShadeRule.rename("shapeless.**" -> "com.abdulradi.opentracing.xray.shaded_libs.shapeless.@1").inAll,
      ShadeRule.rename("atto.**" -> "com.abdulradi.opentracing.xray.shaded_libs.atto.@1").inAll,
      ShadeRule.rename("eu.timepit.refined.**" -> "com.abdulradi.opentracing.xray.shaded_libs.refined.@1").inAll,
      ShadeRule.rename("machinist.**" -> "com.abdulradi.opentracing.xray.shaded_libs.machinist.@1").inAll,
      ShadeRule.rename("macrocompat.**" -> "com.abdulradi.opentracing.xray.shaded_libs.macrocompat.@1").inAll
    ),
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    assemblyMergeStrategy in assembly := {
      case PathList("java", xs @ _*)  => MergeStrategy.discard
      case PathList("javax", xs @ _*)  => MergeStrategy.discard
      case PathList("scala", xs @ _*)  => MergeStrategy.discard
      case PathList("rootdoc.txt")  => MergeStrategy.discard
      case _ => MergeStrategy.deduplicate
    },
    addArtifact(artifact in (Compile, assembly), assembly),
    skip in publish := true
  )

lazy val `shadded-opentracing-xray` = project
  .settings(
    libraryDependencies += Jaeger.core,
    packageBin in Compile := (assembly in (`opentracing-xray-fat`, Compile)).value
  )
*/

addCommandAlias(
  "fmt",
  ";headerCreate;test:headerCreate;sbt:scalafmt;scalafmt;test:scalafmt"
)
