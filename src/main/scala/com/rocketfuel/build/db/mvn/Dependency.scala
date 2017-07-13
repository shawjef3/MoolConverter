package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.build.db.gradle.Library
import com.rocketfuel.sdbc.PostgreSql._

import scala.xml._

case class Dependency(
  sourceId: Int,
  groupId: String,
  artifactId: String,
  version: String,
  scope: String,
  `type`: Option[String]
) {

  lazy val mavenDefinition: Elem =
    <dependency>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      <scope>{scope}</scope>
      {
        if (`type`.isDefined)
          <type>{`type`.get}</type>
      }
    </dependency>

  lazy val gradleDefinition: String = {
    val configuration = scope match {
      case "provided" => "compileOnly"
      case "test" => "testCompile"
      case _ => "compile"
    }
    s"  ${configuration} '${groupId}:${artifactId}:${version}',\n"
  }
}

object Dependency extends Deployable {
  val list =
    Select[Dependency]("SELECT source_id sourceId, group_id groupId, artifact_id artifactId, version, scope, type FROM mvn.all_dependencies")

  val selectBySourceId =
    Select[Dependency](list.originalQueryText + " WHERE source_id = @sourceId")

  val deployDependenciesQuery = Ignore.readClassResource(classOf[Dependency], "dependencies.sql")

  val deployProvidedDependenciesQuery = Ignore.readClassResource(classOf[Dependency], "provided_dependencies.sql")

  val deployAllDependenciesQuery = Ignore.readClassResource(classOf[Dependency], "all_dependencies.sql")

  override def deploy()(implicit connection: Connection): Unit = {
    deployDependenciesQuery.ignore()
    deployProvidedDependenciesQuery.ignore()
    deployAllDependenciesQuery.ignore()
  }

  override def undeploy()(implicit connection: Connection): Unit = {
    Ignore.ignore("DROP VIEW IF EXISTS mvn.dependencies CASCADE")
    Ignore.ignore("DROP VIEW IF EXISTS mvn.provided_dependencies CASCADE")
    Ignore.ignore("DROP VIEW IF EXISTS mvn.all_dependencies CASCADE")
  }
}
