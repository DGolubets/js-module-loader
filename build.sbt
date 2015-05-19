name := "js-module-loader"

organization := "ru.dgolubets"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"
    
scalacOptions ++= Seq("-feature", "-deprecation", "-target:jvm-1.8")

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-classic" % "1.1.1"  % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )

// publishing
publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/DGolubets/js-module-loader"))

pomExtra :=
  <scm>
    <url>git@github.com:DGolubets/js-module-loader.git</url>
    <connection>scm:git:git@github.com:DGolubets/js-module-loader.git</connection>
  </scm>
  <developers>
    <developer>
      <id>DGolubets</id>
      <name>Dmitry Golubets</name>
      <email>dgolubets@gmail.com</email>
    </developer>
  </developers>