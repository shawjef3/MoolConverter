package com.rocketfuel.build.db.mool

import java.nio.file.Path

import com.rocketfuel.build.db._
import com.rocketfuel.build.db.mvn.{Dependency, Identifier, Parents}
import com.rocketfuel.build.mool.MoolPath
import com.rocketfuel.sdbc.PostgreSql._

import scala.xml.Elem

case class Bld(
  id: Int,
  path: Seq[String],
  ruleType: String,
  scalaVersion: Option[String] = None,
  javaVersion: Option[String] = None,
  groupId: Option[String] = None,
  artifactId: Option[String] = None,
  version: Option[String] = None,
  repoUrl: Option[String] = None
) {

  def pom(identifier: Identifier, dependencies: Vector[Dependency], projectRoot: Path, moduleRoot: Path): Elem = {
    val parentArtifact = Parents.parent(this)
    val parentNode = parentArtifact.parentXml(projectRoot, moduleRoot)

    val pomJavaVersion =
      javaVersion.getOrElse("1.8")

    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      {identifier.mavenDefinition}

      {parentNode}

      <dependencies>
        {
        for (dependency <- dependencies) yield
          dependency.mavenDefinition
        }
      </dependencies>

      <properties>
        <maven.compiler.source>{pomJavaVersion}</maven.compiler.source>
        <maven.compiler.target>{pomJavaVersion}</maven.compiler.target>
      </properties>

    </project>
  }

}

object Bld extends Deployable with InsertableToValue[Bld] with SelectableById[Bld] with SelectByPath[Bld] {

  private val columnMapping =
    //Can't use a map, since the column names need to be ordered.
    Seq(
      "id" -> "id",
      "path" -> "path",
      "rule_type" -> "ruleType",
      "scala_version" -> "scalaVersion",
      "java_version" -> "javaVersion",
      "group_id" -> "groupId",
      "artifact_id" -> "artifactId",
      "version" -> "version",
      "repo_url" -> "repoUr"
    )

  private val selectList = {
    for ((sqlName, scalaName) <- columnMapping) yield
      sqlName + " AS " + scalaName
  }.mkString(", ")

  val all =
    Select[Bld](s"SELECT $selectList FROM mool.blds")

  //Blds which aren't references to maven artifacts.
  val localBlds =
    Select[Bld](all.originalQueryText + " WHERE group_id IS NULL")

  override val selectByIdSql: CompiledStatement =
    s"SELECT $selectList FROM blds WHERE id = @id"

  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE mool.blds (
        |  id serial PRIMARY KEY,
        |  path text[] NOT NULL UNIQUE,
        |  rule_type text NOT NULL,
        |  scala_version text,
        |  java_version text,
        |  group_id text,
        |  artifact_id text,
        |  version text,
        |  repo_url text
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mool.blds CASCADE")

  override val insertSql: CompiledStatement =
    """INSERT INTO mool.blds (
      |  path,
      |  rule_type,
      |  scala_version,
      |  java_version,
      |  group_id,
      |  artifact_id,
      |  version,
      |  repo_url
      |) VALUES (
      |  @path,
      |  @ruleType,
      |  @scalaVersion,
      |  @javaVersion,
      |  @groupId,
      |  @artifactId,
      |  @version,
      |  @repoUrl
      |) RETURNING id
      |""".stripMargin

  def create(path: MoolPath, bld: com.rocketfuel.build.mool.Bld)(implicit connection: Connection): Bld = {
    insert(
      Bld(
        id = 0,
        path = path,
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
    Select[Bld](all.originalQueryText + " WHERE path = @path")

}
