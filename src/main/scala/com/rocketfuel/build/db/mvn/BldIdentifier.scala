package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.{Deployable, Listable}
import com.rocketfuel.sdbc.PostgreSql._

case class BldIdentifier(
  bldId: Int,
  groupId: String,
  artifactId: String,
  version: String
)

object BldIdentifier extends Deployable with Listable[BldIdentifier] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE OR REPLACE VIEW mvn.identifiers AS
        |WITH base AS (
        |SELECT
        |  id AS bld_id,
        |  coalesce(
        |    group_id,
        |    array_to_string(
        |      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[2:4]
        |           --for BLDs in java/org/apache/spark
        |           WHEN path[1:4] = array['java', 'org', 'apache', 'spark'] THEN array['com', 'rocketfuel', 'spark']
        |           ELSE array_append(array['com', 'rocketfuel'], path[1])
        |      END,
        |      '.',
        |      NULL
        |    )
        |  ) AS group_id,
        |  coalesce(
        |    artifact_id,
        |    array_to_string(
        |      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[5:array_length(path, 1)]
        |           --for BLDs in java/org/apache/spark
        |           WHEN path[1:4] = array['java', 'org', 'apache', 'spark'] THEN path[5:array_length(path, 1)]
        |           ELSE path[2:array_length(path, 1)]
        |      END,
        |      '.',
        |      NULL
        |    )
        |  ) AS artifact_id,
        |  coalesce(version, 'M1') AS version
        |  FROM blds
        |), counted_duplicates AS (
        |  SELECT
        |    row_number() over (PARTITION BY group_id, artifact_id, version) AS duplicate_id,
        |    base.*
        |  FROM base
        |)
        |SELECT
        |  bld_id,
        |  group_id,
        |  artifact_id ||
        |    CASE WHEN duplicate_id > 1 THEN '_duplicate_' || duplicate_id - 1
        |         ELSE ''
        |    END AS artifact_id,
        |  version
        |FROM counted_duplicates
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.identifiers CASCADE")

  override val listSource: String = "SELECT * FROM mvn.identifiers"

  override protected implicit val rowConverter: RowConverter[BldIdentifier] =
    RowConverter[BldIdentifier]
}
