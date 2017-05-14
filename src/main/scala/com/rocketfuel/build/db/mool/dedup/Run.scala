package com.rocketfuel.build.db.mool.dedup

import com.rocketfuel.sdbc.PostgreSql._

object Run {

  val query = Ignore.readTypeResource[Run.type]("run.sql")

  def run()(implicit connection: Connection): Unit =
    query.ignore()

}
