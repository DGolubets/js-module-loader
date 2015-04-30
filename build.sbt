name := "js-module-loader"

version := "0.0.1"

scalaVersion := "2.11.6"
    
scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )