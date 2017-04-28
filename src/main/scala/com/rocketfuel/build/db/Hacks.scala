package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

object Hacks {
  val query = Ignore.readResource("com/rocketfuel/build/db/hacks.sql")

  def hack()(implicit connection: Connection): Unit =
    query.ignore()
}
