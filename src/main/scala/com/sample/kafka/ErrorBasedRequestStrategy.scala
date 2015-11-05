package com.sample.kafka2es

import akka.stream.actor.RequestStrategy

abstract class ErrorBasedRequestStrategy(max: Int) extends RequestStrategy {

  def isClosed: Boolean
  def isHalfOpen: Boolean

  /**
   * Concrete subclass must implement this method to define how many
   * messages that are currently in progress or queued.
   */
  def inFlightInternally: Int

  /**
   * Elements will be requested in minimum batches of this size.
   * Default is 50. Subclass may override to define the batch size.
   */
  def batchSize: Int = 1000

  override def requestDemand(remainingRequested: Int): Int = {
    val batch = math.min(batchSize, max)
    if (isClosed) {
      if ((remainingRequested + inFlightInternally) <= (max - batch))
        math.max(0, max - remainingRequested - inFlightInternally)
      else 0
    } else (isHalfOpen) ? 1 | 0
  }
}