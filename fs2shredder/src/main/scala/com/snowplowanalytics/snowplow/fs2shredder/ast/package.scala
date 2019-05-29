package com.snowplowanalytics.snowplow.fs2shredder

import ast.CreateTable._
import ast.PostgresDatatype._

import doobie.implicits._
import doobie.util.fragment.Fragment

package object ast {
  trait DoobieSqlFragment {
    def fragment: Fragment
  }

  class DatatypeSqlFragment(ddl: PostgresDatatype) extends DoobieSqlFragment {
    def fragment: Fragment = ddl match {
      case Varchar(Some(size))      => Fragment.const(s"VARCHAR($size)")
      case Varchar(None)            => Fragment.const("VARCHAR")
      case Timestamp                => Fragment.const("TIMESTAMP")
      case Char(size)               => Fragment.const(s"CHAR($size)")
      case SmallInt                 => Fragment.const("SMALLINT")
      case DoublePrecision          => Fragment.const("DOUBLE PRECISION")
      case Integer                  => Fragment.const("INTEGER")
      case Number(precision, scale) => Fragment.const(s"NUMERIC($precision,$scale)")
      case Boolean                  => Fragment.const("BOOLEAN")
      case Json                     => Fragment.const("JSON")
    }
  }
  implicit def postgresDataTypeToFragment(ddl: PostgresDatatype): DatatypeSqlFragment = new DatatypeSqlFragment(ddl)

  class PrimaryKeySqlFragment(ddl: PrimaryKeyConstraint) extends DoobieSqlFragment {
    def fragment: Fragment = fr"CONSTRAINT" ++ Fragment.const(ddl.name) ++ fr"PRIMARY KEY(" ++ Fragment.const(ddl.column) ++ fr")"
  }
  implicit def primaryKeyConstraintToFragment(ddl: PrimaryKeyConstraint): PrimaryKeySqlFragment = new PrimaryKeySqlFragment(ddl)

  class ColumnSqlFragment(ddl: Column) extends DoobieSqlFragment {
    def fragment: Fragment = {
      val datatype = ddl.dataType.fragment
      val notNullConstraint = Option(ddl.notNull).collect { case true => fr"NOT NULL" }.fragment
      val uniqueConstraint = Option(ddl.unique).collect{ case true => fr"UNIQUE" }.fragment
      Fragment.const(ddl.name) ++ datatype ++ notNullConstraint ++ uniqueConstraint
    }
  }
  implicit def columnToFragment(ddl: Column): ColumnSqlFragment = new ColumnSqlFragment(ddl)

  class OptionSqlFragment(opt: Option[Fragment]) extends DoobieSqlFragment {
    def fragment: Fragment = {
      if (opt.nonEmpty) opt.get else fr0""
    }
  }
  implicit def optionToFragment(opt: Option[Fragment]): OptionSqlFragment = new OptionSqlFragment(opt)
}
