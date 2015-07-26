package com.sample.kafka

import akka.stream.actor.RequestStrategy

abstract class ErrorBasedRequestStrategy(highWatermark: Int, lowWatermark: Int) extends RequestStrategy {

  def this(highWatermark: Int) = this(highWatermark, lowWatermark = math.max(1, highWatermark / 2))

  def isClosed: Boolean
  def isHalfOpen: Boolean

  def requestDemand(remainingRequested: Int): Int = {
    if (isClosed) {
      if (remainingRequested < lowWatermark)
        return highWatermark - remainingRequested
      else return 0
    }
    (isHalfOpen) ? 1 | 0
  }
}