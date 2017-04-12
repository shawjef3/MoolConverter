package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.build.mool.MoolPath
import com.rocketfuel.sdbc.PostgreSql
import com.rocketfuel.sdbc.PostgreSql._

case class RelCfg(
  id: Int,
  path: String,
  groupId: String,
  artifactId: String,
  baseVersion: String,
  jarNoDepTarget: Option[Int] = None,
  jarNoDepArtifactPath: Option[String] = None,
  jarWithDepsTarget: Option[Int] = None,
  jarWithDepsArtifactPath: Option[String] = None
)

object RelCfg extends Deployable with InsertableToValue[RelCfg] with SelectableById[RelCfg] with SelectByPath[RelCfg] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore(
      """CREATE TABLE relcfgs (
        |  id serial PRIMARY KEY,
        |  path text NOT NULL UNIQUE,
        |  group_id text NOT NULL,
        |  artifact_id text NOT NULL,
        |  base_version text NOT NULL,
        |  jar_no_dep_target int,
        |  jar_no_dep_artifact_path text,
        |  jar_with_deps_target int,
        |  jar_with_deps_artifact_path text
        |)
        |""".stripMargin
    ).ignore

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore("DROP TABLE IF EXISTS relcfgs").ignore()

  override val insertSql: CompiledStatement =
    """INSERT INTO relcfgs (
      |  path,
      |  group_id,
      |  artifact_id,
      |  base_version,
      |  jar_no_dep_target,
      |  jar_no_dep_artifact_path,
      |  jar_with_deps_target,
      |  jar_with_deps_artifact_path
      |) VALUES (
      |  @path,
      |  @groupId,
      |  @artifactId,
      |  @baseVersion,
      |  @jarNoDepTarget,
      |  @jarNoDepArtifactPath,
      |  @jarWithDepsTarget,
      |  @jarWithDepsArtifactPath
      |) RETURNING id
      |""".stripMargin

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM relcfgs WHERE id = @id"

  override val selectByPathSql: PostgreSql.Select[RelCfg] =
    Select("SELECT * FROM relcfgs WHERE path = @path")

  def create(path: MoolPath, relCfg: com.rocketfuel.build.mool.RelCfg)(implicit connection: Connection): RelCfg = {
    val noDepsBld =
      for (noDeps <- relCfg.`jar-no-dependencies`.map(_.target)) yield {
        //drop(5) removes "mool."
        Bld.selectByPath(noDeps.drop(5))
      }

    val depsBld =
      for (withDeps <- relCfg.`jar-with-dependencies`.map(_.target)) yield {
        //drop(5) removes "mool."
        Bld.selectByPath(withDeps.drop(5))
      }

    insert(
      RelCfg(
        id = 0,
        path = path.mkString("."),
        groupId = relCfg.group_id,
        artifactId = relCfg.artifact_id,
        baseVersion = relCfg.base_version,
        jarNoDepTarget = noDepsBld.map(_.get.id),
        jarNoDepArtifactPath = relCfg.`jar-no-dependencies`.map(_.artifact_path),
        jarWithDepsTarget = depsBld.map(_.get.id),
        jarWithDepsArtifactPath = relCfg.`jar-with-dependencies`.map(_.artifact_path)
      )
    )
  }

}
