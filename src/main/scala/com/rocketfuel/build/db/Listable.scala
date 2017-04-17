package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

trait Listable[A] {

  val listSource: String

  protected implicit val rowConverter: RowConverter[A]

  lazy val listSelect: Select[A] =
    Select[A](s"SELECT * FROM $listSource")

  def select()(implicit connection: Connection): Vector[A] =
    listSelect.vector()

}
