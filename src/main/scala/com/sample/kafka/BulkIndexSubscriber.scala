package com.sample.kafka2es

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.DurationInt

import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue

import com.sample.base.Model
import com.sample.converter.JsonConverter

import akka.actor.ActorLogging
import akka.stream.actor.ActorSubscriberMessage.OnComplete
import akka.stream.actor.ActorSubscriberMessage.OnError
import akka.stream.actor.ActorSubscriberMessage.OnNext

class BulkIndexingSubscriber(client: Client,
                             listener: ResponseListener = ResponseListener.noop,
                             batchSize: Int = 1500,
                             concurrentRequests: Int = 20,
                             refreshAfterOp: Boolean = false,
                             completionFn: () => Unit,
                             errorFn: Throwable => Unit) extends EnhancedActorSubscriber with ActorLogging {

  import context.dispatcher
  context.system.scheduler.schedule(0.milli, 1.minute, self, "geterrorcount")
  var errorCnt: AtomicLong = new AtomicLong(0)

  val bulkListener = new BulkProcessor.Listener() {
    def beforeBulk(id: Long, request: BulkRequest) {
    }

    def afterBulk(executionId: Long,
                  request: BulkRequest,
                  response: BulkResponse) = {
      response.getItems.foreach(listener.onAck)
      if (response.hasFailures()) {
        response.getItems foreach { e =>
          if (e.isFailed()) {
            errorCnt.getAndIncrement
            println(e.getFailureMessage)
          }
        }
      }
    }

    def afterBulk(executionId: Long,
                  request: BulkRequest,
                  failure: Throwable) = errorFn(failure)
  }

  val processor = BulkProcessor.builder(client, bulkListener).
    setBulkActions(batchSize).
    setConcurrentRequests(concurrentRequests).
    setFlushInterval(TimeValue.timeValueSeconds(1)).
    build()

  def receive = {
    case OnNext(model: Model) =>
      processor.add(new IndexRequest(model.store, model.entity).source(JsonConverter.convert(model).getBytes))

    case OnError(t: Throwable) => errorFn(t)

    case OnComplete            => shutdownIfAllAcked()

    case "geterrorcount"       => log.info("Number of errors/min : " + errorCnt)

    case _                     =>
  }

  private def shutdownIfAllAcked() {
    completionFn()
    context.stop(self)
  }
}

trait ResponseListener {
  def onAck(resp: BulkItemResponse): Unit
}

object ResponseListener {
  def noop = new ResponseListener {
    override def onAck(resp: BulkItemResponse): Unit = ()
  }
}