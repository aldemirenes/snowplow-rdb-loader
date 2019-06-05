package com.snowplowanalytics.snowplow.fs2shredder

import fs2.{Stream, io, text}
import cats.implicits._
import cats.effect._

import scala.collection.JavaConverters._
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.concurrent.Executors

import cats.Monad
import com.snowplowanalytics.iglu.client.Resolver
import com.snowplowanalytics.iglu.client.resolver.registries.RegistryLookup

import scala.concurrent.ExecutionContext
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.rdbloader.common.{Common, EventUtils, Shredded}

object Loader {

  private val blockingExecutionContext =
    Resource.make(IO(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))))(ec => IO(ec.shutdown()))

  def list(path: Path): Stream[IO, Path] =
    Stream
      .fromIterator[IO, Path](Files.list(path).iterator().asScala)
      .filter(p => !p.getFileName.toString.startsWith("."))

  def parseEvent(eventStr: String): Event = {
    println(s"Event: $eventStr")
    Event.parse(eventStr).valueOr(error => throw new RuntimeException(error.toList.mkString(", ")))
  }

  def shred[F[_]: Monad: RegistryLookup: Clock](base: Path, resolver: Resolver[F])(event: Event): F[List[(Path, String)]] = {
    val atomic = EventUtils.alterEnrichedEvent(event, Map())
    val shredded = Common.getShreddedEntities(event)
      .traverse { hierarchy => Shredded.fromHierarchy[F](false, resolver)(hierarchy).map(_.tabular) }
      .map(_.unite)
      .map { s => s.map { case (vendor, name, format, version, data) => (getShredPath(base, vendor, name, format, version), data) } }
    shredded.map { data => (Paths.get(base.toAbsolutePath.toString, "atomic-events", "part-0"), atomic) :: data }
  }

  def getShredPath(base: Path, vendor: String, name: String, format: String, version: String) =
    Paths.get(base.toAbsolutePath.toString, "shredded-tsv", s"vendor=$vendor", s"name=$name", s"format=$format", s"version=$version", "part-0")


  def create(file: Path): IO[Unit] =
    IO {
      if (Files.exists(file)) () else {
        Files.createDirectories(file.getParent)
        Files.createFile(file)
        println(s"Created $file")
      }
    }

  def run(loaderConfig: Config)(implicit ce: ConcurrentEffect[IO], cs: ContextShift[IO], c: Clock[IO]): Stream[IO, Unit] = {
    val resources = for {
      ec <- blockingExecutionContext
      iglu <- Resource.liftF(Utils.loadResolver[IO](loaderConfig.igluConfig))
    } yield (ec, iglu)

    val process = Stream.resource(resources)
      .flatMap { case (ec, iglu) =>
        val events = NsqSource.run[IO](loaderConfig.nsq).map(parseEvent)
        events.evalMap(shred[IO](loaderConfig.outputFolder, iglu))
          .flatMap(rows => Stream.emits(rows))
          .evalTap { case (path, _) => create(path) }
          .flatMap { case (path, row) =>
            val sink = io.file.writeAll[IO](path, ec, Seq(StandardOpenOption.WRITE, StandardOpenOption.APPEND))
            val input = Stream.emit(row ++ "\n").through(text.utf8Encode)
            input.through(sink)
          }
      }
    process
  }

}
