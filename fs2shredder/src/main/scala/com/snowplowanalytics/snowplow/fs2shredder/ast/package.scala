package com.snowplowanalytics.snowplow.fs2shredder

package object ast {

  implicit class StatementSyntax[S](val ast: S) extends AnyVal {
    def getStatement(implicit S: Statement[S]): Statement.SqlStatement =
      S.getStatement(ast)
  }
}
