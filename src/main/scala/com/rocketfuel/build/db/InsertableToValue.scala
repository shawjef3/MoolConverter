package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._
import shapeless._
import shapeless.ops.hlist.ReplaceAt
import shapeless.ops.product.ToHList
import shapeless.syntax.std.product._

trait InsertableToValue[A <: Product] {

  val insertSql: CompiledStatement

  /**
    * Where A is a case class where the first value is an Int,
    * replace it with the one returned by the insert statement.
    */
  def insert[
    Source <: HList,
    Source2 <: HList,
    Dest <: HList,
    Key <: Symbol,
    AsParameters <: HList
  ](value: A
  )(implicit connection: Connection,
    rowConverter: RowConverter[Int],
    p: Parameters.Products[A, Source, Key, AsParameters],
    toHList: ToHList.Aux[A, Source2],
    replacer: ReplaceAt.Aux[Source2, Nat._0, Int, (Int, Dest)],
    generic: Generic.Aux[A, Dest]
  ): A = {
    replaceId(value, Select[Int](insertSql).onProduct(value).one())
  }

  def replaceId[
    Source <: HList,
    Dest <: HList
  ](value: A,
    id: Int
  )(implicit toHList: ToHList.Aux[A, Source],
    replacer: ReplaceAt.Aux[Source, Nat._0, Int, (Int, Dest)],
    generic: Generic.Aux[A, Dest]
  ): A =
    generic.from(value.toHList.updatedAt(Nat._0, id))

}
