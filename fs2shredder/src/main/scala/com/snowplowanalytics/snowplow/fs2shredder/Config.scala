package com.snowplowanalytics.snowplow.fs2shredder

import java.nio.file.Path

import cats.implicits._

import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

import com.monovore.decline._

case class Config(nsq: Config.NsqConfig,
                  postgres: Config.DbConfig,
                  outputFolder: Path,
                  igluConfig: Path)

object Config {

  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  val loaderConfigFile = Opts.option[Path]("loader-config", "Loader config file", metavar = "file")

  val loader = Opts.subcommand("loader", "Transform and load the coming events")(loaderConfigFile.map(LoaderCommand.Run.apply))

  val setup = Opts.subcommand("setup", "Setup database")(loaderConfigFile.map(LoaderCommand.Setup.apply))

  val command = Command(s"fs2-shredder-0.1.0",
    "App to prepare Snowplow enriched data to being loaded into Amazon Redshift warehouse")(loader.orElse(setup))

  sealed trait LoaderCommand {
    def config: Path
    def read: Either[String, Config] =
      pureconfig.loadConfigFromFiles[Config](List(config)).leftMap(_.toList.map(_.description).mkString("\n"))
  }

  object LoaderCommand {
    case class Run(config: Path) extends LoaderCommand
    case class Setup(config: Path) extends LoaderCommand
  }

  case class NsqConfig(topic: String,
                       channel: String,
                       host: Utils.HttpUrl,
                       port: Int,
                       lookupHost: Utils.HttpUrl,
                       lookupPort: Int)

  case class DbConfig(host: Utils.HttpUrl,
                      port: Int,
                      dbname: String,
                      username: String,
                      password: String)
}
