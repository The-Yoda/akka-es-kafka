name := "akka-es-kafka"

organization in ThisBuild := "com.sample.kafka"

scalaVersion in ThisBuild := "2.11.7"

version in ThisBuild := "0.1.0-SNAPSHOT"

val akkaVersion = "2.4.0"

resolvers ++= Seq(
  "public snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "org.elasticsearch" % "elasticsearch" % "1.7.2",
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.8.2",
  "org.apache.kafka" %% "kafka" % "0.8.2.2"
)
