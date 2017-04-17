package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object BldToTestBld extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW mool.bld_to_test_bld AS
        |SELECT
        |  bld_to_bld.target_id AS source_id,
        |  bld_to_bld.source_id AS target_id
        |FROM mool.test_blds
        |INNER JOIN mool.bld_to_bld
        |  ON test_blds.id = bld_to_bld.source_id
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mool.bld_to_test_bld CASCADE")
}
