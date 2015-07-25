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

class EsProxy extends ActorProcessor[Model] {

  import context.dispatcher

  def receive = {
    case OnNext(data: Try[Model]) =>
      val optype = "add"
      if (data.isFailure) println("FAILURE :: " + data.failed)
      else {
        val msg = data.get
        context.actorOf(Props(classOf[EsStorage], self)) ! msg
      }

    case OnError(err: Exception) =>
      println("ERROR :: " + err)

    case OnComplete =>
      context.stop(self)

    case msg: Model =>
      val content = gen(msg.getContent())
      if (bool(content.hasFailed()))
        requestStrategy.breaker.withCircuitBreaker(Future(throw new Exception("FAIL")))
      if (isActive && totalDemand > 0)
        onNext(content)
      println("In response : " + msg)
  }
}