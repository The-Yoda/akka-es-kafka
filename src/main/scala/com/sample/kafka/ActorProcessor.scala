package com.sample.kafka

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorSubscriber
import akka.pattern.CircuitBreaker
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import akka.actor.ActorContext

trait ActorProcessor[T] extends ActorSubscriber with ActorPublisher[T] {
  var isCircuitOpen = false
  var isCircuitHalfOpen = false
  var isCircuitClosed = true

  val requestStrategy = new ErrorBasedRequestStrategy(5) {
    def isClosed: Boolean = isCircuitClosed
    def isHalfOpen: Boolean = isCircuitHalfOpen
  }

  import context.dispatcher
  val breaker =
    new CircuitBreaker(context.system.scheduler,
      maxFailures = 3,
      callTimeout = 10.seconds,
      resetTimeout = 10.seconds)

  // request demand of 0 when circuit is open from upstream
  breaker.onOpen({
    updateStates(true, false, false)
    request(requestStrategy.requestDemand(remainingRequested))
  })

  breaker.onClose(updateStates(false, true, false))

  // request demand of 1 when circuit is open from upstream
  breaker.onHalfOpen({
    updateStates(false, false, true)
    request(requestStrategy.requestDemand(remainingRequested))
  })

  def updateStates(isOpen: Boolean, isClosed: Boolean, isHalfOpen: Boolean) = {
    isCircuitOpen = isOpen
    isCircuitClosed = isClosed
    isCircuitHalfOpen = isHalfOpen
  }
}