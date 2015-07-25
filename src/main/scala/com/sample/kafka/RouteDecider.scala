package com.sample.kafka

import akka.stream.Attributes
import akka.stream.FanOutShape
import akka.stream.FanOutShape.Init
import akka.stream.FanOutShape.Name
import akka.stream.scaladsl.FlexiRoute
import akka.stream.scaladsl.FlexiRoute.DemandFromAll
import akka.stream.scaladsl.FlexiRoute.RouteLogic

/**
 * Determines shape of outlet.
 * Two outlet ports(ack, err) with same type as input(A) is being used.
 */
class ResultShape[A](_init: Init[A] = Name[A]("Split"))
  extends FanOutShape[A](_init) {
  // Add extra outlets, if any
  val ack = newOutlet[A]("ack")
  val err = newOutlet[A]("err")
  protected override def construct(i: Init[A]) = new ResultShape(i)
}

/**
 * Decides route based on input and sends stream to specified outlet port
 */
class RouteDecider[A] extends FlexiRoute[A, ResultShape[A]](
  new ResultShape, Attributes.name("Split")) {
  import FlexiRoute._

  /**
   * Route logic which tells to which outlet port the data needs to be sent for further processing
   */
  override def createRouteLogic(p: PortT) = new RouteLogic[A] {
    override def initialState =
      State[Any](DemandFromAll(p.ack, p.err)) {
        (ctx, _, element) =>
          //Add route logic on input element
          if (bool((gen(element).hasFailed())))
            ctx.emit(p.ack)(element)
          else
            ctx.emit(p.err)(element)
          SameState
      }

    override def initialCompletionHandling = eagerClose
  }
}