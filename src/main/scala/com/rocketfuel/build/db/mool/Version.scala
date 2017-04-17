package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._

case class Version(
  id: Int,
  path: Seq[String],
  artifactId: String,
  commit: String,
  version: String
)

object Version extends  Deployable with SelectableById[Version] with InsertableToValue[Version] {
  val list =
    Select[BldToSource]("SELECT * FROM mool.versions")

  override val insertSql: CompiledStatement =
    """INSERT INTO mool.versions (
      |  path, artifact_id, commit, version
      |) VALUES (
      |  @path, @artifactId, @commit, @version
      |) RETURNING id
      |""".stripMargin

  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE mool.versions (
        |  id serial PRIMARY KEY,
        |  path text[] NOT NULL,
        |  artifact_id text NOT NULL,
        |  commit text NOT NULL,
        |  version text NOT NULL
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mool.versions")

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM mool.versions WHERE id = @id"
}
