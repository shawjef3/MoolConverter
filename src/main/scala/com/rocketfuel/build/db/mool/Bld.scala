package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.build.mool.MoolPath
import com.rocketfuel.sdbc.PostgreSql._

case class Bld(
  id: Int,
  path: String,
  ruleType: String,
  scalaVersion: Option[String] = None,
  javaVersion: Option[String] = None,
  artifactId: Option[String] = None,
  groupId: Option[String] = None,
  version: Option[String] = None,
  repoUrl: Option[String] = None
) {

}

object Bld extends Deployable with InsertableToValue[Bld] with SelectableById[Bld] with SelectByPath[Bld] {

  override val selectByIdSql: CompiledStatement = "SELECT * FROM blds WHERE id = @id"

  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE blds (
        |  id serial PRIMARY KEY,
        |  path text NOT NULL UNIQUE,
        |  rule_type text NOT NULL,
        |  scala_version text,
        |  java_version text,
        |  artifact_id text,
        |  group_id text,
        |  version text,
        |  repo_url text
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS blds")

  override val insertSql: CompiledStatement =
    """INSERT INTO blds (
      |  path,
      |  rule_type,
      |  scala_version,
      |  java_version,
      |  artifact_id,
      |  group_id,
      |  version,
      |  repo_url
      |) VALUES (
      |  @path,
      |  @ruleType,
      |  @scalaVersion,
      |  @javaVersion,
      |  @artifactId,
      |  @groupId,
      |  @version,
      |  @repoUrl
      |) RETURNING id
      |""".stripMargin

  def create(path: MoolPath, bld: com.rocketfuel.build.mool.Bld)(implicit connection: Connection): Bld = {
    insert(
      Bld(
        id = 0,
        path = path.mkString("."),
        ruleType = bld.rule_type,
        javaVersion = bld.java_version,
        scalaVersion = bld.scala_version,
        artifactId = bld.maven_specs.map(_.artifact_id),
        groupId = bld.maven_specs.map(_.group_id),
        version = bld.maven_specs.map(_.version),
        repoUrl = bld.maven_specs.map(_.repo_url)
      )
    )
  }

  val selectByPathSql: Select[Bld] =
    Select("SELECT * FROM blds WHERE path = @path")

}
