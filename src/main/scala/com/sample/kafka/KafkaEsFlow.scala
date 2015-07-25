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
      val group = topic + "group"
      val esActor = system.actorOf(Props(classOf[EsProxy]))
      getPublisherFlow(Source(ActorPublisher[Model](esActor)), topic, group).run()
      getConsumerFlow(Sink(ActorSubscriber[Try[Model]](esActor)), topic, group).run()
  }

  def getConsumerFlow(sink: Sink[Try[Model], Unit], topic: String, group: String) = {
    val kafkaConsumer = Source(Kafka.kafka.consume(topic, topic + "group", new StringDecoder()))
    val flow = Flow[String].map { elem => Try(JsonConverter.convert(elem.toString())) }
    kafkaConsumer.via(flow).to(sink)
  }

  def getPublisherFlow(source: Source[Model, Unit], topic: String, group: String) = {
    val ackQueue = Sink(Kafka.kafka.publish("ack" + topic, group, new StringEncoder()))
    val errorQueue = Sink(Kafka.kafka.publish("error" + topic, group, new StringEncoder()))

    FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
      import FlowGraph.Implicits._
      val routeDecider = builder.add(new RouteDecider[Model])
      source ~> routeDecider.in
      routeDecider.ack.map { gen => JsonConverter.convert(gen) } ~> ackQueue
      routeDecider.err.map { gen => JsonConverter.convert(gen) } ~> errorQueue
    }
  }
}