package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class BldJoins(id: Int, path: Seq[String], addedId: Int, includeType: String)

object BldJoins extends Deployable with Logger {
  val list = Select[BldJoins]("SELECT id, path, added_id as addedId, include_type AS includeType FROM gradle.bld_joins")

  val deployQuery = Ignore.readClassResource(classOf[BldJoins], "bld_joins.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.bld_joins CASCADE")
}
