package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object Schema extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore("CREATE SCHEMA IF NOT EXISTS mool")

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP SCHEMA IF EXISTS mool CASCADE")
}
