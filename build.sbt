import ProjectPluginKeys._

import com.abdulradi.Dependencies._

scalaVersion in ThisBuild := "2.12.3"
crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.3")

lazy val `opentracing-xray` =
  (project in file("."))
    .enableProjectPlugin()
    .settings(
      libraryDependencies ++= Seq(
        atto,
        Jaeger.core % Provided,
        Refined.core % Provided,
        circe,
        circeRefined,
        scalaTest % Test,
        circeParser % Test
      ) 
    )

lazy val `opentracing-xray-fat` = project
  .enablePlugins(AssemblyPlugin)
  .dependsOn(`opentracing-xray`)
  .settings(
    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("cats.**" -> "com.abdulradi.opentracing.xray.shaded_libs.cats.@1").inAll,
      ShadeRule.rename("io.circe.**" -> "com.abdulradi.opentracing.xray.shaded_libs.cats.io.circe.@1").inAll,
      ShadeRule.rename("shapeless.**" -> "com.abdulradi.opentracing.xray.shaded_libs.shapeless.@1").inAll,
      ShadeRule.rename("atto.**" -> "com.abdulradi.opentracing.xray.shaded_libs.atto.@1").inAll,
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
      case PathList("eu", "timepit", "refined", xs @ _*)  => MergeStrategy.discard
      case PathList("rootdoc.txt")  => MergeStrategy.discard
      case _ => MergeStrategy.deduplicate
    },
    addArtifact(artifact in (Compile, assembly), assembly),
    skip in publish := true
  )

lazy val `shadded-opentracing-xray` = project
  .settings(
    packageBin in Compile := (assembly in (`opentracing-xray-fat`, Compile)).value
  )

addCommandAlias(
  "fmt",
  ";headerCreate;test:headerCreate;sbt:scalafmt;scalafmt;test:scalafmt"
)

// while you're working, try putting "~wip" into your sbt console
// ...but be prepared to let IntelliJ force you to reload your source code!
addCommandAlias("wip", ";fmt;test:compile")
