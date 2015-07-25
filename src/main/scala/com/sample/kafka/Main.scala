package com.sample.kafka

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.parseString(
    s"""
       |squbs {
       |  external-config-dir = squbsconfig
       |}
    """.stripMargin)

  implicit val system = ActorSystem("kafka", config)

  system.actorOf(Props[KafkaEsFlow]) ! "start"
}