package com.snowplowanalytics.snowplow.fs2shredder

import cats.data.EitherT
import cats.implicits._
import cats.effect._

import fs2.Stream

import com.snowplowanalytics.snowplow.fs2shredder.storage.Storage

object Main extends IOApp {
  import scala.concurrent.ExecutionContext.Implicits.global

  def run(args: List[String]): IO[ExitCode] = {

    val cli = for {
      command <- EitherT.fromEither[IO](Config.command.parse(args).leftMap(_.toString))
      config <- EitherT.fromEither[IO](command.read)
      result <- {
        val res = for {
          storage <- Stream.resource(Storage.initialize[IO](config.postgres))
          res <- command match {
            case _: Config.LoaderCommand.Run =>
              Loader.run(config)
            case _: Config.LoaderCommand.Setup =>
              Stream.eval(Initializer.run(config.postgres, storage))
          }
        } yield res
        EitherT.liftF[IO, String, ExitCode](res.compile.drain.as(ExitCode.Success))
      }
    } yield result

    cli.value.flatMap {
      case Right(code) => IO.pure(code)
      case Left(cliError) => IO(System.err.println(cliError)).as(ExitCode.Error)
    }
  }
}
