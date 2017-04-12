package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

trait SelectableById[A] {

  val selectByIdSql: CompiledStatement

  def selectById(
    id: Int
  )(implicit connection: Connection,
    rowConverter: RowConverter[A]
  ): Option[A] = {
    Select[A](selectByIdSql).on("id" -> id).option()
  }

}
