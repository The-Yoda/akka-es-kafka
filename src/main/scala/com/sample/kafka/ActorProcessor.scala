package com.sample.kafka

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorSubscriber

trait ActorProcessor[T] extends ActorSubscriber with ActorPublisher[T] {
  val requestStrategy = ErrorBasedRequestStrategy(5, context)
}