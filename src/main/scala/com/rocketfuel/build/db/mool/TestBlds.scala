package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object TestBlds extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW mool.test_blds AS
        |SELECT *
        |FROM mool.blds
        |WHERE rule_type LIKE '%test%'
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mool.test_blds CASCADE")
}
