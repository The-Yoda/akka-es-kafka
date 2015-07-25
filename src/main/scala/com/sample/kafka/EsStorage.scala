package com.sample.kafka

import akka.actor.Actor
import akka.actor.ActorLogging
import com.sample.base.Model
import akka.actor.ActorRef

class EsStorage(origin: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case msg: Model =>
      msg.setContent(new Model(Map("failed" -> true)))
      origin ! msg
  }
}