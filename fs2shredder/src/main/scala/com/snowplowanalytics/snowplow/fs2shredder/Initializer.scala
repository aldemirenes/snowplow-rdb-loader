package com.snowplowanalytics.snowplow.fs2shredder

import cats.implicits._
import cats.effect.{Clock, IO}
import com.snowplowanalytics.snowplow.fs2shredder.Config.DbConfig
import com.snowplowanalytics.snowplow.fs2shredder.ast.{AtomicDef, Defaults}
import com.snowplowanalytics.snowplow.fs2shredder.storage.{Postgres, Storage}

object Initializer {

  def run(config: DbConfig, storage: Storage[IO])(implicit C: Clock[IO]): IO[Unit] = {
    val allStatements = List(
      storage.createSchema(Defaults.Schema),
      storage.createTable(AtomicDef.getTable())
    )
    allStatements
      .sequence
      .void
  }
}
