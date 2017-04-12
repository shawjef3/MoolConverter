package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object SourceConflicts extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW source_conflicts AS
        |WITH conflicted AS (
        |  SELECT source_id AS conflicted_id
        |  FROM source_to_blds
        |  GROUP BY source_id
        |  HAVING count(*) > 1
        |)
        |SELECT source_id, bld_id
        |FROM source_to_blds
        |WHERE EXISTS (
        |  SELECT 1
        |  FROM conflicted
        |  WHERE conflicted_id = source_id
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS source_conflicts CASCADE")
}
