package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.{Deployable, Listable}
import com.rocketfuel.sdbc.PostgreSql._

case class Dependency(
  bldId: Int,
  groupId: String,
  artifactId: String,
  version: String,
  scope: String
) {

}

object Dependency extends Deployable with Listable[Dependency] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE OR REPLACE VIEW mvn.dependencies AS
        |SELECT
        |  bld_to_bld.source_id,
        |  bld_to_bld.target_id,
        |  identifiers.group_id,
        |  identifiers.artifact_id,
        |  identifiers.version,
        |  CASE WHEN blds.rule_type like '%_test' THEN 'test' --BLDs only have one scope
        |       ELSE 'main'
        |  END AS scope
        |FROM mool.bld_to_bld
        |INNER JOIN mool.blds
        |  ON bld_to_bld.target_id = blds.id
        |INNER JOIN mvn.identifiers
        |  ON identifiers.bld_id = bld_to_bld.target_id
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.dependencies CASCADE")

  override val listSource: String =
    "SELECT * FROM mvn.dependencies"

  override protected implicit val rowConverter: RowConverter[Dependency] =
    RowConverter[Dependency]
}
