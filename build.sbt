name := "js-module-loader"

organization := "ru.dgolubets"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"
    
scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "ch.qos.logback" % "logback-classic" % "1.1.1"  % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )