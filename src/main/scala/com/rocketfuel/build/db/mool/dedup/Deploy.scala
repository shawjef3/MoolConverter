package com.rocketfuel.build.db.mool.dedup

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object Deploy extends Deployable {
  val createQuery =
    Ignore.readTypeResource[Deploy.type]("create.sql")

  override def deploy()(implicit connection: Connection): Unit =
    createQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP SCHEMA IF EXISTS mool_dedup CASCADE")
}
