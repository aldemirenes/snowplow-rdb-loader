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

object NsqSource {

  private def createNsqSuccessCallback(onComplete: Either[Throwable, String] => Unit): NSQMessageCallback =
    new NSQMessageCallback {
      override def message(msg: NSQMessage): Unit = {
        val bytes = msg.getMessage()
        val str = new String(bytes, StandardCharsets.UTF_8)
        if (!str.trim.isEmpty) onComplete(Right(str))
        msg.finished()
        println(s"message: $str")
      }
    }

  private def createNsqErrorCallback(onComplete: Either[Throwable, String] => Unit): NSQErrorCallback =
    new NSQErrorCallback {
      override def error(e: NSQException): Unit = {
        println(s"error: $e")
        onComplete(Left(new Throwable(e)))
      }
    }

  private def enqueue[F[_]: ContextShift](q: Queue[F, Either[Throwable, String]])(implicit F: ConcurrentEffect[F]) =
    (e: Either[Throwable, String]) => F.runAsync(q.enqueue1(e))(_ => IO.unit).unsafeRunSync

  private def startNsqSource[F[_]: ConcurrentEffect : ContextShift](config: NsqConfig, q: Queue[F, Either[Throwable, String]]) = {
    val nsqSuccessCallback = createNsqSuccessCallback(enqueue(q))
    val nsqErrorCallback = createNsqErrorCallback(enqueue(q))
    // use NSQLookupd
    val lookup = new DefaultNSQLookup
    lookup.addLookupAddress(config.lookupHost.toString, config.lookupPort)
    val consumer = new NSQConsumer(
      lookup,
      config.topic,
      config.channel,
      nsqSuccessCallback,
      new NSQConfig(),
      nsqErrorCallback)
    consumer.start()
  }

  def run[F[_]: ContextShift](config: NsqConfig)(implicit F: ConcurrentEffect[F]): Stream[F, String] = {
    for {
      q <- Stream.eval(Queue.unbounded[F,Either[Throwable,String]])
      _ <-  Stream.eval(F.delay(startNsqSource(config, q)))
      event <- q.dequeue.rethrow
    } yield event
  }
}
