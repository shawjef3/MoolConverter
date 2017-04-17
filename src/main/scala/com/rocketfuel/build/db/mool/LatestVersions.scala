package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class LatestVersions(
  bldId: String,
  path: Seq[String],
  artifactId: String,
  version: String
)

object LatestVersions extends Deployable {
  val list =
    Select[LatestVersions]("SELECT * FROM mool.latest_versions")

  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW mool.latest_versions AS
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
    Ignore.ignore("DROP VIEW IF EXISTS mool.latest_versions CASCADE")
}
