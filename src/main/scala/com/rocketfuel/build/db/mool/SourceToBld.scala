package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object SourceToBld extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW mool.source_to_blds AS
        |SELECT
        |  sources.id AS source_id,
        |  blds.id AS bld_id
        |FROM mool.blds
        |INNER JOIN mool.bld_to_sources
        |  ON blds.id = bld_to_sources.bld_id
        |INNER JOIN sources
        |  ON bld_to_sources.source_id = sources.id
        |GROUP BY sources.id, blds.id
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mool.source_to_blds CASCADE")
}
