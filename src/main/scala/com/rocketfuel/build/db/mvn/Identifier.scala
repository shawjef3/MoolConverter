package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Identifier(
  bldId: Int,
  groupId: String,
  artifactId: String,
  version: String
)

object Identifier extends Deployable {
  val list =
    Select[Identifier]("SELECT bld_id bldId, group_id groupId, artifact_id artifactId, version FROM mvn.identifiers")

  val deployQuery = Ignore.readClassResource(classOf[Identifier], "identifiers.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.identifiers CASCADE")
}
