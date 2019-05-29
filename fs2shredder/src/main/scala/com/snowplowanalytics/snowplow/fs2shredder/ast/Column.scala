package com.snowplowanalytics.snowplow.fs2shredder.ast

case class Column(
 name: String,
 dataType: PostgresDatatype,
 notNull: Boolean = false,
 unique: Boolean = false)