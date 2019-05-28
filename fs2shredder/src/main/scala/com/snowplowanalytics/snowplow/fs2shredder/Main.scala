package com.snowplowanalytics.snowplow.fs2shredder

import cats.data.EitherT
import cats.implicits._
import cats.effect._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val cli = for {
      command <- EitherT.fromEither[IO](Config.command.parse(args).leftMap(_.toString))
      config <- EitherT.fromEither[IO](command.read)
      result <- command match {
        case _: Config.LoaderCommand.Run =>
          EitherT.liftF[IO, String, ExitCode](Loader.run(config))
        case _: Config.LoaderCommand.Setup =>
          EitherT.liftF[IO, String, ExitCode](Initializer.run())
      }
    } yield result

    cli.value.flatMap {
      case Right(code) => IO.pure(code)
      case Left(cliError) => IO(System.err.println(cliError)).as(ExitCode.Error)
    }
  }
}
