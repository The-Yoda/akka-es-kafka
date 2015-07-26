package com.sample.kafka

import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.stream.actor.ActorSubscriber
import akka.stream.actor.ActorSubscriberMessage.OnComplete
import akka.stream.actor.ActorSubscriberMessage.OnError
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.WatermarkRequestStrategy
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import scala.util.Try
import akka.pattern.CircuitBreaker
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import com.sample.base.Model
import akka.stream.actor.ActorPublisherMessage

class EsProxy extends ActorProcessor[Model] {

  import context.dispatcher

  def receive = {
    case OnNext(data: Try[Model]) =>
      if (data.isFailure) println("FAILURE :: " + data.failed)
      else {
        val msg = data.get
        context.actorOf(Props(classOf[EsStorage], self)) ! msg
      }

    case OnError(err: Exception) =>
      println("ERROR :: " + err)

    case OnComplete =>
      context.stop(self)

    case ActorPublisherMessage.Request(x) => println("Demand : " + x)
    case ActorPublisherMessage.Cancel | ActorPublisherMessage.SubscriptionTimeoutExceeded =>
      context.stop(self)

    case msg: Model =>
      val content = gen(msg.getContent())
      if (bool(content.hasFailed()))
        breaker.withCircuitBreaker(Future(throw new Exception("FAIL")))
      else breaker.withCircuitBreaker(Future(true))
      
      if (isActive && totalDemand > 0) onNext(content)
      println("In response : " + msg)
  }
}