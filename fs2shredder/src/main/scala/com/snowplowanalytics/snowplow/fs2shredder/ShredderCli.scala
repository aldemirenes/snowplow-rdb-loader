package com.snowplowanalytics.snowplow.fs2shredder

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import cats.effect.Sync
import cats.implicits._
import io.circe.jawn.parse
import com.monovore.decline._
import com.snowplowanalytics.iglu.client.resolver.Resolver
import org.apache.commons.codec.binary.Base64

/**
  * Case class representing the configuration for the shred job.
 *
  * @param outFolder Output folder where the shredded events will be stored
  * @param badFolder Output folder where the malformed events will be stored
  * @param igluConfig JSON representing the Iglu configuration
  * @param jsonOnly don't try to produce TSV output
  */
case class ShredderCli(topic: String,
                       channel: String,
                       host: String,
                       port: Int,
                       lookupHost: String,
                       lookupPort: Int,
                       outFolder: Path,
                       badFolder: Path,
                       igluConfig: Path,
                       jsonOnly: Boolean) {
}

object ShredderCli {

  val topic = Opts.option[String]("topic",
    "Nsq Topic")
  val channel = Opts.option[String]("channel",
    "Nsq Channel")
  val host = Opts.option[String]("host",
    "Nsq Host")
  val port = Opts.option[Int]("port",
    "Nsq Port")
  val lookupHost = Opts.option[String]("lookupHost",
    "Nsq Lookup Host")
  val lookupPort = Opts.option[Int]("lookupPort",
    "Nsq Lookup Port")
  val outputFolder = Opts.option[Path]("output-folder",
    "Output folder where the shredded events will be stored",
    metavar = "<path>")
  val badFolder = Opts.option[Path]("bad-folder",
    "Output folder where the malformed events will be stored",
    metavar = "<path>")
  val igluConfig = Opts.option[Path]("iglu-config",
    "Base64-encoded Iglu Client JSON config",
    metavar = "<file>")

  val jsonOnly = Opts.flag("json-only", "Do not produce tabular output").orFalse

  val shredJobConfig = (topic, channel, host, port, lookupHost, lookupPort, outputFolder, badFolder, igluConfig, jsonOnly).mapN {
    (topic, channel, host, port, lookupHost, lookupPort, output, bad, iglu, jsonOnly) => ShredderCli(topic, channel, host, port, lookupHost, lookupPort, output, bad, iglu, jsonOnly)
  }


  val command = Command(s"fs2-shredder-0.1.0",
    "App to prepare Snowplow enriched data to being loaded into Amazon Redshift warehouse")(shredJobConfig)

  def loadResolver[F[_]: Sync](path: Path): F[Resolver[F]] =
    Sync[F].delay { new String(Files.readAllBytes(path), StandardCharsets.UTF_8) }
      .map { arg => new String(new Base64(true).decode(arg), StandardCharsets.UTF_8) }
      .map { str => parse(str).valueOr(throw _) }
      .flatMap { json => Resolver.parse[F](json) }
      .map { x => x.valueOr(throw _) }

}
