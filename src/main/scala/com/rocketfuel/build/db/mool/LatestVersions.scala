package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.{Deployable, Listable}
import com.rocketfuel.sdbc.PostgreSql._

case class LatestVersions(
  bldId: String,
  path: Vector[String],
  artifactId: String,
  version: String
)

object LatestVersions extends Deployable with Listable[LatestVersions] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW latest_versions AS
        |SELECT bld.id AS bld_id, path, artifact_id, max(version) AS version
        |FROM mool.relcfg_to_bld
        |INNER JOIN mool.bld
        |  ON bld.id = relcfg_to_bld.bld_id
        |INNER JOIN mool.versions
        |  ON bld.path =
        |GROUP BY path, artifact_id
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS latest_versions CASCADE")

  override val listSource: String =
    "SELECT * FROM latest_versions"

  override protected implicit val rowConverter: RowConverter[LatestVersions] =
    RowConverter[LatestVersions]
}
