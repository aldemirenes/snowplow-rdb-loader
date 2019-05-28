package com.snowplowanalytics.snowplow.fs2shredder

import cats.effect.{ExitCode, IO}

object Initializer {

  def run(): IO[ExitCode]= IO.pure(ExitCode.Success)
}
