package com.sample.kafka2es

import scala.concurrent.duration.DurationInt

import akka.pattern.CircuitBreaker
import akka.stream.actor.ActorSubscriber

trait EnhancedActorSubscriber extends ActorSubscriber {
  var isCircuitOpen = false
  var isCircuitHalfOpen = false
  var isCircuitClosed = true
  var inflightRequests = 0

  val requestStrategy = new ErrorBasedRequestStrategy(1000) {
    def isClosed: Boolean = isCircuitClosed
    def isHalfOpen: Boolean = isCircuitHalfOpen
    override def inFlightInternally: Int = inflightRequests
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