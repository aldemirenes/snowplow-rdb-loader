//package com.snowplowanalytics.snowplow.fs2shredder.connection
//
//import com.snowplowanalytics.snowplow.fs2shredder.ast.Statement
//
//trait Connection {
//  trait Connection[C] {
//    def getConnection(config: Config): C
//    def execute[S: Statement](connection: C, ast: S): Unit
//    def startTransaction(connection: C, name: Option[String]): Unit
//    def commitTransaction(connection: C): Unit
//    def rollbackTransaction(connection: C): Unit
//    def executeAndOutput[S: Statement](connection: C, ast: S): Unit
//    def executeAndCountRows[S: Statement](connection: C, ast: S): Int
//    def executeAndReturnResult[S: Statement](connection: C, ast: S): List[Map[String, Object]]
//  }
//}
