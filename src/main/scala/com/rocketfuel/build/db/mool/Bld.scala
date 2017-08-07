package com.rocketfuel.build.db.mool

import java.nio.file.Path
import com.rocketfuel.build.db._
import com.rocketfuel.build.db.gradle.GradleConvert
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
    exclusions: Map[Int, Map[Int, Set[Exclusion]]]
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
          dependency.mavenDefinition(exclusions.getOrElse(id, Map.empty))
        }
      </dependencies>

      <properties>
        <maven.compiler.source>{pomJavaVersion}</maven.compiler.source>
        <maven.compiler.target>{pomJavaVersion}</maven.compiler.target>
      </properties>

      {
      //BUILD_ROOT part of the fix for athena testdata
      if (path.contains("athena")) {
        <build>
          <plugins>
            <plugin>
              <groupId>me.jeffshaw.scalatest</groupId>
              <artifactId>scalatest-maven-plugin</artifactId>
              <version>2.0.0-M1</version>
              <configuration>
                <environmentVariables>
                  <BUILD_ROOT>
                  {
                  //work around for needing a literal ${} inside of xml.
                  xml.Text("${basedir}")
                  }
                  </BUILD_ROOT>
                </environmentVariables>
              </configuration>
            </plugin>
          </plugins>
        </build>
      }
      }

      {
      //fix for resources being loaded using the file system instead of as resources.
      if (Bld.requiresTestWithExtractedDependencies.contains(path)) {
        Bld.testWithExtractedDependencies
      }
      }

    </project>
  }

  def gradle(
    identifier: Identifier,
    dependencies: Vector[Dependency],
    projectRoot: Path,
    moduleRoot: Path,
    modulePaths: Map[Int, String],
    moduleOutputs: Map[String, Int]
  ): String = {
    GradleConvert.gradle(
      identifier = identifier,
      prjBld = this,
      dependencies = dependencies,
      projectRoot = projectRoot,
      moduleRoot = moduleRoot,
      modulePaths = modulePaths,
      moduleOutputs = moduleOutputs
    )
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
  val locals =
    Select[Bld](all.originalQueryText + " WHERE group_id IS NULL")

  val athenaTests =
    Select[Bld](
      locals.originalQueryText +
        """ AND rule_type LIKE '%test'
          |AND path[1:5] = array['java', 'com', 'rocketfuel', 'modeling', 'athena']
          |""".stripMargin
    )

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

  val requiresTestWithExtractedDependencies =
    Set(
      Seq("java", "com", "rocketfuel", "grid", "lookup", "dim", "DimTablesTest"),
      Seq("java", "com", "rocketfuel", "grid", "common", "hbase", "keylistformat", "filefetcher", "KeyValueFileFetcherTest")
    )

  val testWithExtractedDependencies: xml.Elem = {
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-dependency-plugin</artifactId>
          <executions>
            <execution>
              <phase>process-test-resources</phase>
              <goals>
                <goal>unpack-dependencies</goal>
              </goals>
              <configuration>
                <includeGroupIds>com.rocketfuel.grid</includeGroupIds>
                <excludes>
                {
                //work around for needing a literal /* instead of xml.
                xml.Text("**/*.class")
                }
                </excludes>
              </configuration>
            </execution>
          </executions>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <configuration>
            <!-- run the tests with the unpacked dependencies in the classpath. -->
            <!-- see https://maven.apache.org/plugins/maven-dependency-plugin/unpack-dependencies-mojo.html -->
            <workingDirectory>
              {
              //work around for needing a literal ${} inside of xml.
              xml.Text("${project.build.directory}/dependency")
              }
            </workingDirectory>
          </configuration>
        </plugin>
      </plugins>
    </build>
  }

}
