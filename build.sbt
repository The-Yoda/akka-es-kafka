name := "akka-es-kafka"

organization in ThisBuild := "com.sample.kafka"

scalaVersion in ThisBuild := "2.11.7"

version in ThisBuild := "0.1.0-SNAPSHOT"

val akkaVersion = "2.3.12"

resolvers ++= Seq(
  "public snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC4",
  "com.softwaremill" %% "reactive-kafka" % "0.8.0-SNAPSHOT",
  "org.apache.kafka" %% "kafka" % "0.8.2.1"
)
