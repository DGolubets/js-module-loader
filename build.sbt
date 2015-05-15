name := "js-module-loader"

organization := "ru.dgolubets"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"
    
scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-classic" % "1.1.1"  % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )