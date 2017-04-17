package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.{Deployable, SelectableById}
import com.rocketfuel.sdbc.PostgreSql._

case class ModulePath(
  id: Int,
  path: String
)

object ModulePath extends Deployable with SelectableById[ModulePath] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE VIEW mvn.module_paths AS (
        |  SELECT
        |    id,
        |    array_to_string(
        |      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
        |           ELSE path
        |      END,
        |      '/',
        |      NULL
        |    ) AS path
        |  FROM blds
        |  WHERE array[path[1], path[2]] != array['java', 'mvn']
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.module_paths CASCADE")

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM mvn.module_paths WHERE id = @id"
}
