package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.{Deployable, Listable}
import com.rocketfuel.sdbc.PostgreSql
import com.rocketfuel.sdbc.PostgreSql._

case class Copy(
  source: String,
  destination: String
)

object Copy extends Deployable with Listable[Copy] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW mvn.copies AS (
        |  WITH dir_parts AS (
        |  SELECT
        |    module_paths.path AS module_path,
        |    sources.path AS source,
        |    CASE WHEN blds.rule_type like '%_test' THEN 'test'
        |         ELSE 'main'
        |    END AS config_path,
        |    CASE WHEN sources.path LIKE '%.scala' THEN 'scala'
        |         WHEN sources.path LIKE '%.java' THEN 'java'
        |         WHEN sources.path LIKE '%.py' THEN 'python'
        |         ELSE 'resources'
        |    END AS lang_path
        |  FROM mool.blds
        |  INNER JOIN mvn.module_paths
        |    ON blds.id = module_paths.id
        |  INNER JOIN mool.bld_to_sources
        |    ON blds.id = bld_to_sources.bld_id
        |  INNER JOIN mool.sources
        |    ON bld_to_sources.source_id = sources.id
        |  )
        |  SELECT
        |    source,
        |    module_path || array_to_string(array['', 'src', config_path, lang_path, ''], '/', NULL) ||
        |      CASE WHEN source LIKE 'java/%' THEN substring(source from 6 for (char_length(source) - 6))
        |           ELSE source
        |      END AS destination
        |  FROM dir_parts
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.copies CASCADE")


  override protected implicit val rowConverter: PostgreSql.RowConverter[Copy] =
    RowConverter[Copy]

  override val listSource: String = "mvn.copies"
}
