package com.sample.kafka

import akka.stream.actor.RequestStrategy
import akka.pattern.CircuitBreaker
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import akka.actor.ActorContext

object ErrorBasedRequestStrategy {
  def apply(highWatermark: Int, context: ActorContext): ErrorBasedRequestStrategy = new ErrorBasedRequestStrategy(highWatermark, context)
}

case class ErrorBasedRequestStrategy(highWatermark: Int, lowWatermark: Int, context: ActorContext) extends RequestStrategy {

  def this(highWatermark: Int, context: ActorContext) = this(highWatermark, lowWatermark = math.max(1, highWatermark / 2), context)

  import context.dispatcher

  var isCircuitOpen = false
  var isCircuitHalfOpen = false
  var isCircuitClosed = true

  val breaker =
    new CircuitBreaker(context.system.scheduler,
      maxFailures = 3,
      callTimeout = 10.seconds,
      resetTimeout = 10.seconds)
  breaker.onOpen(updateStates(true, false, false))
  breaker.onClose(updateStates(false, true, false))
  breaker.onHalfOpen(updateStates(false, false, true))

  def updateStates(isOpen: Boolean, isClosed: Boolean, isHalfOpen: Boolean) = {
    if (isOpen) println("ON open")
    if (isHalfOpen) println("In Half open")
    if (isClosed) println("In closed state")
    isCircuitOpen = isOpen
    isCircuitClosed = isClosed
    isCircuitHalfOpen = isHalfOpen
  }

  def requestDemand(remainingRequested: Int): Int = {
    println("Request demand state: " + isCircuitOpen + " c : " + isCircuitClosed + " HO : " + isCircuitHalfOpen)
    if (isCircuitClosed) {
      if (remainingRequested < lowWatermark)
        return highWatermark - remainingRequested
      else return 0
    }
    (isCircuitHalfOpen) ? 1 | 0
  }
}