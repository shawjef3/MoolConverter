package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object Schema extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore("CREATE SCHEMA IF NOT EXISTS mvn")

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP SCHEMA IF EXISTS mvn CASCADE")
}
