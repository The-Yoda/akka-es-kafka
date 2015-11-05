package com.sample.kafka2es

import scala.util.Try
import com.sample.base.Model
import com.sample.base._
import com.sample.converter.JsonConverter
import com.softwaremill.react.kafka.ReactiveKafka
import com.softwaremill.react.kafka.KafkaMessages.StringKafkaMessage
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
import akka.stream.Supervision
import akka.stream.ActorMaterializerSettings
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import com.softwaremill.react.kafka.ConsumerProperties
import scala.concurrent.duration.DurationInt

object Transformer {
  var config: Model = null
  var kafka: ReactiveKafka = null
  var kafkaHosts: String = null
  var zookeeperHosts: String = null
  var esHosts: String = null
  var client: Client = null
  var topics: List[String] = null
}

class KafkaEsFlow extends Actor with ActorLogging {

  implicit val system = context.system
  val decider: Supervision.Decider = { case _ => Supervision.Resume }
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  import context.dispatcher
  context.system.scheduler.schedule(0.milli, 1.minute, self, "getcount")
  var count: Long = 0
  var previousSum: Long = 0

  def init(config: Model) {
    Transformer.kafkaHosts = "localhost:9200"
    Transformer.zookeeperHosts = "localhost:2181"
    Transformer.esHosts = "localhost:9300"
    Transformer.kafka = new ReactiveKafka()
    Transformer.client = getEsClient
    Transformer.topics = List("sample")
  }

  def receive = {
    case "init" =>
      for (topic <- Transformer.topics) {
        log.info("create flow for topic " + topic)
        val esAsSink = Sink(ActorSubscriber[Model](system.actorOf(Props(
          classOf[BulkIndexingSubscriber],
          getEsClient,
          ResponseListener.noop,
          2000,
          100,
          false,
          () => println("stream complete"),
          (t: Throwable) => println(t)))))
        getFlow(esAsSink, topic).run()
      }

    case _ =>
  }

  def getEsClient = {

    import org.elasticsearch.common.settings.ImmutableSettings
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "samplecluster").build()
    var transportClient = new TransportClient(settings)
    Transformer.esHosts split (",") foreach { e =>
      var address = e.split(":")
      val transportAddress = new InetSocketTransportAddress(address(0), address(1).toInt)
      transportClient = transportClient.addTransportAddress(transportAddress)
    }
    transportClient.asInstanceOf[Client]
  }

  var getEsRequest = (input: StringKafkaMessage) => {
    JsonConverter.convert(input.message)
  }

  def getFlow(esAsSink: Sink[Model, Unit], topic: String) = {
    val source = Source(Transformer.kafka.consume(ConsumerProperties(
      brokerList = Transformer.kafkaHosts,
      zooKeeperHost = Transformer.zookeeperHosts,
      topic = topic,
      groupId = topic + "group",
      decoder = new StringDecoder())))

    val kafkaToEs = Flow[StringKafkaMessage].map(getEsRequest)

    FlowGraph.closed() {
      implicit builder: FlowGraph.Builder[Unit] =>
        source ~> kafkaToEs ~> esAsSink
    }
  }
}