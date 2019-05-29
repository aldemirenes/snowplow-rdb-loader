package com.snowplowanalytics.snowplow.fs2shredder.storage

import cats.Monad
import cats.effect._
import doobie.hikari._
import doobie.util.ExecutionContexts
import com.snowplowanalytics.snowplow.fs2shredder.Config.DbConfig
import com.snowplowanalytics.snowplow.fs2shredder.ast.CreateTable

import scala.concurrent.ExecutionContext

trait Storage[F[_]] {
  def createSchema(schemaName: String)(implicit C: Clock[F], M: Monad[F]): F[Unit]
  def createTable(ddl: CreateTable)(implicit C: Clock[F], M: Monad[F]): F[Unit]
}

object Storage {

  def initialize[F[_]: Effect: ContextShift](config: DbConfig)
                                            (implicit transactEC: ExecutionContext): Resource[F, Storage[F]] = {
    val url = s"jdbc:postgresql://${config.host}:${config.port}/${config.dbname}"
    for {
      connectEC <- ExecutionContexts.fixedThreadPool(config.connectThreads.getOrElse(32))
      xa <- HikariTransactor.newHikariTransactor[F](config.driver, url, config.username, config.password, connectEC, transactEC)
      _ <- Resource.liftF {
        xa.configure { ds =>
          Sync[F].delay {
            ds.setMaximumPoolSize(config.maxPoolSize.getOrElse(10))
          }
        }
      }
      storage <- Resource.liftF(Postgres(xa).ping)
    } yield storage
  }
}
