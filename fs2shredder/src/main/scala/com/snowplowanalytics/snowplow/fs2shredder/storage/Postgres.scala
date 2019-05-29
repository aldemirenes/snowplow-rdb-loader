package com.snowplowanalytics.snowplow.fs2shredder.storage

import cats.implicits._
import cats.Monad
import cats.effect.{Clock, IO}

import doobie._
import doobie.implicits._
import doobie.Transactor
import doobie.util.update.Update0

import com.snowplowanalytics.snowplow.fs2shredder.ast.CreateTable
import com.snowplowanalytics.snowplow.fs2shredder.ast._

class Postgres[F[_]](xa: Transactor[F]) extends Storage[F] { self =>
  def createSchema(schemaName: String)(implicit C: Clock[F], M: Monad[F]): F[Unit] = {
    Postgres.Sql.createSchema(schemaName).run.void.transact(xa)
  }

  def createTable(ddl: CreateTable)(implicit C: Clock[F], M: Monad[F]): F[Unit] = {
    Postgres.Sql.createTable(ddl).run.void.transact(xa)
  }

  def ping(implicit F: Monad[F]): F[Storage[F]] =
    sql"SELECT 42".query[Int].unique.transact(xa).as(self)
}

object Postgres {

  def apply[F[_]](xa: Transactor[F]): Postgres[F] = new Postgres(xa)

  object Sql {
    def createSchema(schemaName: String): Update0 = {
      (fr"CREATE SCHEMA IF NOT EXISTS" ++ Fragment.const0(schemaName)).update
    }

    def createTable(ddl: CreateTable): Update0 = {
      val constraint: Option[Fragment] = ddl.primaryKey.map(_.fragment)
      val cols = ddl.columns.map(_.fragment).foldRight(Fragment.empty)(_ ++ fr"," ++ _ )
      (fr"CREATE TABLE IF NOT EXISTS"
        ++ Fragment.const0(ddl.schema) ++ fr0"." ++ Fragment.const(ddl.name)
        ++ fr"(" ++ cols ++ constraint.fragment ++ fr")"
      ).update
    }
  }
}
