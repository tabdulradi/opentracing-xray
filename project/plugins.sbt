ivyLoggingLevel := UpdateLogging.Quiet
scalacOptions in Compile ++= Seq("-feature", "-deprecation")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "4.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")