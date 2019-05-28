package com.snowplowanalytics.snowplow.fs2shredder

import com.snowplowanalytics.client.nsq.lookup.DefaultNSQLookup
import com.snowplowanalytics.client.nsq.{NSQConfig, NSQConsumer, NSQMessage, NSQProducer}
import com.snowplowanalytics.client.nsq.callbacks.NSQMessageCallback
import com.snowplowanalytics.client.nsq.callbacks.NSQErrorCallback
import com.snowplowanalytics.client.nsq.exceptions.NSQException
import java.nio.charset.StandardCharsets

import cats.effect.{Async, ConcurrentEffect, ContextShift, IO}
import com.snowplowanalytics.snowplow.fs2shredder.Config.NsqConfig
import fs2.concurrent.Queue
import fs2.{Stream, io, text}

class NsqSource[F[_]](config: NsqConfig)(implicit F: ConcurrentEffect[F], cs: ContextShift[F]) {

  class NsqCallbacks(onComplete: Either[Throwable, String] => Unit) {
    val successCallback = new NSQMessageCallback {
      override def message(msg: NSQMessage): Unit = {
        val bytes = msg.getMessage()
        val str = new String(bytes, StandardCharsets.UTF_8)
        if (!str.trim.isEmpty) onComplete(Right(str))
        msg.finished()
        println(s"message: $str")
      }
    }

    val errorCallback = new NSQErrorCallback {
      override def error(e: NSQException): Unit = {
        println(s"error: $e")
        onComplete(Left(new Throwable(e)))
      }
    }
  }

  private def enqueue(q: Queue[F, Either[Throwable, String]]) =
    (e: Either[Throwable, String]) => F.runAsync(q.enqueue1(e))(_ => IO.unit).unsafeRunSync

  private def startNsqSource(q: Queue[F, Either[Throwable, String]]) = {
    val nsqCallbacks = new NsqCallbacks(enqueue(q))
    // use NSQLookupd
    val lookup = new DefaultNSQLookup
    lookup.addLookupAddress(config.lookupHost.toString, config.lookupPort)
    val consumer = new NSQConsumer(
      lookup,
      config.topic,
      config.channel,
      nsqCallbacks.successCallback,
      new NSQConfig(),
      nsqCallbacks.errorCallback)
    consumer.start()
  }

  def run(): Stream[F, String] = {
    for {
      q <- Stream.eval(Queue.unbounded[F,Either[Throwable,String]])
      _ <-  Stream.eval(F.delay(startNsqSource(q)))
      event <- q.dequeue.rethrow
    } yield event
  }
}
