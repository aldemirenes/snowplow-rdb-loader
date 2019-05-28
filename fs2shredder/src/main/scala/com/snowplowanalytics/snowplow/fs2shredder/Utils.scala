package com.snowplowanalytics.snowplow.fs2shredder

import cats.implicits._
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import io.circe.jawn.parse

import cats.effect.Sync
import com.snowplowanalytics.iglu.client.resolver.Resolver
import org.apache.commons.codec.binary.Base64

object Utils {

  case class HttpUrl(uri: URI) extends AnyVal {
    override def toString: String = uri.toString
  }

  object HttpUrl {
    /**
      * Parse registry root (HTTP URL) from string with default `http://` protocol
      * @param url string representing just host or full URL of registry root.
      *            Registry root is URL **without** /api
      * @return either error or URL tagged as HTTP in case of success
      */
    def parse(url: String): Either[Throwable, HttpUrl] =
      Either.catchNonFatal {
        if (url.startsWith("http://") || url.startsWith("https://")) {
          HttpUrl(new URI(url.stripSuffix("/")))
        } else {
          HttpUrl(new URI("http://" + url.stripSuffix("/")))
        }
      }
  }

  def loadResolver[F[_]: Sync](path: Path): F[Resolver[F]] =
    Sync[F].delay { new String(Files.readAllBytes(path), StandardCharsets.UTF_8) }
      .map { arg => new String(new Base64(true).decode(arg), StandardCharsets.UTF_8) }
      .map { str => parse(str).valueOr(throw _) }
      .flatMap { json => Resolver.parse[F](json) }
      .map { x => x.valueOr(throw _) }

}
