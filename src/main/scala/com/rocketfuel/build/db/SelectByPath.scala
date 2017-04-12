package com.rocketfuel.build.db

import com.rocketfuel.build.mool.MoolPath
import com.rocketfuel.sdbc.PostgreSql._

trait SelectByPath[A] {

  val selectByPathSql: Select[A]

  def selectByPath(path: String)(implicit connection: Connection): Option[A] =
    selectByPathSql.on("path" -> path).option()

  def selectByPath(path: MoolPath)(implicit connection: Connection): Option[A] =
    selectByPath(path.mkString("."))

}
