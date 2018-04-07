import java.net.URL

name := "js-module-loader"

organization := "ru.dgolubets"

scalaVersion := "2.12.5"
crossScalaVersions := List("2.11.6", "2.12.5")
releaseCrossBuild := true

scalacOptions ++= Seq("-feature", "-deprecation", "-target:jvm-1.8")

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "ch.qos.logback" % "logback-classic" % "1.1.1"  % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  )

// publishing
bintrayRepository := "releases"
bintrayOrganization in bintray := Some("dgolubets")
bintrayPackageLabels := Seq("js", "nashorn", "module", "loader")
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/DGolubets/js-module-loader"))
publishMavenStyle := true
publishArtifact in Test := false
developers := List(Developer(
  "dgolubets",
  "Dmitry Golubets",
  "dgolubets@gmail.com",
  new URL("https://github.com/DGolubets")))