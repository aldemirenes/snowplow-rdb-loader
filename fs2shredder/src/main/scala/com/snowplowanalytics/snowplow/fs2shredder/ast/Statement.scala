package com.snowplowanalytics.snowplow.fs2shredder.ast

trait Statement[-S] {
  def getStatement(ast: S): Statement.SqlStatement
}


object Statement {
  final case class SqlStatement private(value: String) extends AnyVal
}
