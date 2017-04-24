package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class BldAlias(
  sourceId: Int,
  targetId: Int
)

object BldAlias extends Deployable {

  val deployQuery =
    Ignore.readClassResource(classOf[BldAlias], "bld_aliases.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mool.aliases CASCADE")

}
