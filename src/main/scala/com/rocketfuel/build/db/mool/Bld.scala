package com.rocketfuel.build.db.mool

import java.nio.file.Path
import com.rocketfuel.build.db._
import com.rocketfuel.build.db.mvn.{Dependency, Exclusion, Identifier, Parents}
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
  repoUrl: Option[String] = None,
  classifier: Option[String] = None,
  filePackage: Option[String] = None
) {

  def pom(
    identifier: Identifier,
    dependencies: Vector[Dependency],
    projectRoot: Path,
    moduleRoot: Path,
    exclusions: Map[Int, Set[Exclusion]]
  ): Elem = {
    val parentArtifact = Parents.parent(this)
    val parentNode = parentArtifact.parentXml(projectRoot, moduleRoot)

    val pomJavaVersion =
      javaVersion.getOrElse("1.8")

    //this helps with finding the bld in postgresql
    val moolPathComment =
      xml.Comment("mool path: " + path.mkString("['", "', '", "']"))

    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>
      {moolPathComment}

      {identifier.mavenDefinition}

      {parentNode}

      <dependencies>
        {
        for (dependency <- dependencies) yield
          dependency.mavenDefinition(exclusions)
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
      "repo_url" -> "repoUr",
      "classifier" -> "classifier",
      "file_package" -> "filePackage"
    )

  private val selectList = {
    for ((sqlName, scalaName) <- columnMapping) yield
      sqlName + " AS " + scalaName
  }.mkString(", ")

  val all =
    Select[Bld](s"SELECT $selectList FROM mool_dedup.blds")

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
        |  repo_url text,
        |  classifier text,
        |  file_package text
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
      |  repo_url,
      |  classifier,
      |  file_package
      |) VALUES (
      |  @path,
      |  @ruleType,
      |  @scalaVersion,
      |  @javaVersion,
      |  @groupId,
      |  @artifactId,
      |  @version,
      |  @repoUrl,
      |  @classifier,
      |  @filePackage
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
        repoUrl = bld.maven_specs.map(_.repo_url),
        classifier = bld.maven_specs.flatMap(_.classifier),
        filePackage = bld.file_package
      )
    )
  }

  val selectByPathSql: Select[Bld] =
    Select[Bld](all.originalQueryText + " WHERE path = @path")

}
