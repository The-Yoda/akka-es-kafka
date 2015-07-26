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
  breaker.onOpen({
    updateStates(true, false, false)
    request(requestStrategy.requestDemand(remainingRequested))
  })
  breaker.onClose(updateStates(false, true, false))
  breaker.onHalfOpen({
    updateStates(false, false, true)
    val x = requestStrategy.requestDemand(remainingRequested)
    println(x)
    request(x)
  })

  def updateStates(isOpen: Boolean, isClosed: Boolean, isHalfOpen: Boolean) = {
    if (isOpen) println("ON open")
    if (isHalfOpen) println("In Half open")
    if (isClosed) println("In closed state")
    isCircuitOpen = isOpen
    isCircuitClosed = isClosed
    isCircuitHalfOpen = isHalfOpen
  }
}