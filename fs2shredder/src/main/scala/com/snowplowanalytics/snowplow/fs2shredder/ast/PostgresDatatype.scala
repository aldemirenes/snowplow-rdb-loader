package com.snowplowanalytics.snowplow.fs2shredder.ast

sealed trait PostgresDatatype

object PostgresDatatype {
  case class Varchar(size: Option[Int]) extends PostgresDatatype
  case object Timestamp extends PostgresDatatype
  case class Char(size: Int) extends PostgresDatatype
  case object SmallInt extends PostgresDatatype
  case object DoublePrecision extends PostgresDatatype
  case object Integer extends PostgresDatatype
  case class Number(precision: Int, scale: Int) extends PostgresDatatype
  case object Boolean extends PostgresDatatype
  case object Json extends PostgresDatatype
}
