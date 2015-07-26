package com.sample.kafka

import scala.util.Try

import com.sample.base.Model
import com.sample.converter.JsonConverter
import com.softwaremill.react.kafka.ReactiveKafka

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.FlowGraph
import akka.stream.scaladsl.FlowGraph.Implicits.SourceArrow
import akka.stream.scaladsl.FlowGraph.Implicits.port2flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import kafka.serializer.StringDecoder
import kafka.serializer.StringEncoder

object Kafka {
  var kafka: ReactiveKafka = null
}

class KafkaEsFlow extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  def initKafka = {
    val kafkaHosts = "localhost:9092"
    val zookeeperHosts = "localhost:2181"
    Kafka.kafka = new ReactiveKafka(host = kafkaHosts, zooKeeperHost = zookeeperHosts)
  }

  def receive = {
    case "start" =>
      initKafka
      val topic = "sample"
      val esActor = system.actorOf(Props(classOf[EsProxy]))

      val processorSink = Sink(ActorSubscriber[Try[Model]](esActor))
      val processorSource = Source(ActorPublisher[Model](esActor))
      val kafkaConsumer = Source(Kafka.kafka.consume(topic, topic + "group", new StringDecoder()))
      getFlow(kafkaConsumer, processorSink, processorSource, topic).run()
  }

  def getFlow(source: Source[String, Unit], pSink: Sink[Try[Model], Unit], pSource: Source[Model, Unit], topic: String) = {
    val flow = Flow[String].map { elem => Try(JsonConverter.convert(elem.toString())) }
    val ackQueue = Sink(Kafka.kafka.publish("ack" + topic, getId, new StringEncoder()))
    val errorQueue = Sink(Kafka.kafka.publish("error" + topic, getId, new StringEncoder()))

    FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
      import FlowGraph.Implicits._
      val routeDecider = builder.add(new RouteDecider[Model])
      source ~> flow ~> pSink
      pSource ~> routeDecider.in
      routeDecider.ack.map { gen => JsonConverter.convert(gen) } ~> ackQueue
      routeDecider.err.map { gen => JsonConverter.convert(gen) } ~> errorQueue
    }
  }
}