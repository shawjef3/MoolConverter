package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql
import com.rocketfuel.sdbc.PostgreSql._

case class RelCfg(
  id: Int,
  path: Seq[String],
  groupId: String,
  artifactId: String,
  baseVersion: String
)

object RelCfg extends Deployable with InsertableToValue[RelCfg] with SelectableById[RelCfg] with SelectByPath[RelCfg] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE mool.relcfgs (
        |  id serial PRIMARY KEY,
        |  path text[] NOT NULL UNIQUE,
        |  group_id text NOT NULL,
        |  artifact_id text NOT NULL,
        |  base_version text NOT NULL
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mool.relcfgs")

  override val insertSql: CompiledStatement =
    """INSERT INTO mool.relcfgs (
      |  path,
      |  group_id,
      |  artifact_id,
      |  base_version
      |) VALUES (
      |  @path,
      |  @groupId,
      |  @artifactId,
      |  @baseVersion
      |) RETURNING id
      |""".stripMargin

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM relcfgs WHERE id = @id"

  override val selectByPathSql: PostgreSql.Select[RelCfg] =
    Select[RelCfg]("SELECT * FROM relcfgs WHERE path = @path")

}
