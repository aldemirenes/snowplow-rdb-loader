//package com.snowplowanalytics.snowplow.fs2shredder.connection
//
//import com.snowplowanalytics.snowplow.fs2shredder.ast.Statement
//import com.snowplowanalytics.snowplow.fs2shredder.ast._
//
//class DryRun {
//  private val messages = collection.mutable.ListBuffer.newBuilder[String]
//
//  private var transaction: Option[String] = None
//  private var transactionNum = 0
//
//  def log[S: Statement](ast: S) = {
//    val message = ast.getStatement.value
//    println(message)
//    messages += message
//  }
//
//  def log(message: String) = {
//    val fullMessage = s"INFO: $message"
//    println(fullMessage)
//    messages += fullMessage
//  }
//
//  def startTransaction(name: Option[String]): Unit =
//    transaction match {
//      case Some(current) =>
//        log(s"Invalid state: new transaction started until current [$current] not commited")
//      case None =>
//        log(s"New transaction ${name.getOrElse(" ")} started")
//        transactionNum += 1
//        val transactionName = name.getOrElse(transactionNum.toString)
//        transaction = Some(name.getOrElse(transactionName))
//    }
//
//  def commitTransaction: Unit =
//    transaction match {
//      case Some(current) =>
//        transaction = None
//        log(s"Transaction [$current] successfully closed\n")
//      case None =>
//        log("Invalid state: trying to close non-existent transaction")
//    }
//
//  def rollbackTransaction: Unit =
//    transaction match {
//      case Some(current) =>
//        log(s"Transaction [$current] cancelled")
//      case None =>
//        log("Invalid state: trying to rollback non-existent transaction")
//    }
//
//  def getResult: List[String] =
//    messages.result().toList
//}
//
//object DryRun extends Connection[DryRun] {
//  def getConnection(config: Config): DryRun = {
//    val logConnection = new DryRun
//    logConnection.log(s"Connected to ${config.database} database")
//    logConnection
//  }
//
//  def execute[S: Statement](connection: DryRun, ast: S): Unit =
//    connection.log(ast)
//
//  def startTransaction(connection: DryRun, name: Option[String]): Unit =
//    connection.startTransaction(name)
//
//  def commitTransaction(connection: DryRun): Unit =
//    connection.commitTransaction
//
//  def executeAndOutput[S: Statement](connection: DryRun, ast: S): Unit =
//    connection.log(ast)
//
//  def rollbackTransaction(connection: DryRun): Unit =
//    connection.rollbackTransaction
//
//  def executeAndCountRows[S: Statement](connection: DryRun, ast: S): Int = {
//    connection.log(ast)
//    1 // Used for preliminary checks
//  }
//
//  def executeAndReturnResult[S: Statement](connection: DryRun, ast: S): List[Map[String, Object]] = {
//    connection.log(ast)
//    List.empty
//  }
//}

