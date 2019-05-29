package com.snowplowanalytics.snowplow.fs2shredder.ast

import com.snowplowanalytics.snowplow.fs2shredder.ast.CreateTable._

case class CreateTable(
                        schema: String,
                        name: String,
                        columns: List[Column],
                        primaryKey: Option[PrimaryKeyConstraint],
                        temporary: Boolean = false)

object CreateTable {
  case class PrimaryKeyConstraint(name: String, column: String)
}

