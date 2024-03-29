package com.datatech.sdp.mongo.engine

import java.util.concurrent.atomic.AtomicBoolean

import org.mongodb.{scala => mongoDB}
import org.{reactivestreams => rxStreams}

final case class ObservableToPublisher[T](observable: mongoDB.Observable[T])
  extends rxStreams.Publisher[T] {
  def subscribe(subscriber: rxStreams.Subscriber[_ >: T]): Unit =
    observable.subscribe(new mongoDB.Observer[T]() {
      override def onSubscribe(subscription: mongoDB.Subscription): Unit =
        subscriber.onSubscribe(new rxStreams.Subscription() {
          private final val cancelled: AtomicBoolean = new AtomicBoolean

          override def request(n: Long): Unit =
            if (!subscription.isUnsubscribed && !cancelled.get() && n < 1) {
              subscriber.onError(
                new IllegalArgumentException(
                  s"Demand from publisher should be a positive amount. Current amount is:$n"
                )
              )
            } else {
              subscription.request(n)
            }

          override def cancel(): Unit =
            if (!cancelled.getAndSet(true)) subscription.unsubscribe()
        })

      def onNext(result: T): Unit = subscriber.onNext(result)

      def onError(e: Throwable): Unit = subscriber.onError(e)

      def onComplete(): Unit = subscriber.onComplete()
    })
}
