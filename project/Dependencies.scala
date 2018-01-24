package com.abdulradi

import sbt._

/**
  * Dependencies referenced in project auto-plugin and build code
  */
object Dependencies {
  object Jaeger {
    val core = "com.uber.jaeger" % "jaeger-core" % "0.21.0"
  }

  val atto = "org.tpolecat" %% "atto-core"  % "0.6.1"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4"
  val circe = "io.circe" %% "circe-core" % "0.9.1"
  val circeParser = "io.circe" %% "circe-parser" % "0.9.0-M3"
  val circeRefined = "io.circe" %% "circe-refined" % "0.9.0-M3"

  val mock: ModuleID = "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0"
  val typesafeConfig: ModuleID = "com.typesafe" % "config" % "1.3.1"

  object Refined {
    val version: String = "0.8.2"

    val core: ModuleID = "eu.timepit" %% "refined" % version
    val scalacheck: ModuleID = "eu.timepit" %% "refined-scalacheck" % version
  }
}
