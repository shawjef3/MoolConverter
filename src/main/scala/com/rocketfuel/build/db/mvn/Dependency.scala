package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._
import scala.xml._

case class Dependency(
  sourceId: Int,
  groupId: String,
  artifactId: String,
  version: String,
  scope: String,
  classifier: Option[String]
) {

  lazy val mavenDefinition: Elem =
    <dependency>
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>
      <scope>{scope}</scope>
      {
        if (classifier.contains("test"))
          <type>test-jar</type>
      }
    </dependency>

}

object Dependency extends Deployable {
  val list =
    Select[Dependency](
      """SELECT source_id::int sourceId, group_id::text groupId, artifact_id::text artifactId, version::text, scope::text, classifier::text FROM mvn.dependencies
        |UNION
        |SELECT source_id::int sourceId, group_id::text groupId, artifact_id::text artifactId, version::text, scope::text, classifier::text FROM mvn.provided_dependencies
      """.stripMargin
    )

  val selectBySourceId =
    Select[Dependency](list.queryText + " WHERE source_id = @sourceId")

  val deployDependenciesQuery = Ignore.readClassResource(classOf[Dependency], "dependencies.sql")

  val deployProvidedDependenciesQuery = Ignore.readClassResource(classOf[Dependency], "provided_dependencies.sql")

  override def deploy()(implicit connection: Connection): Unit = {
    deployDependenciesQuery.ignore()
    deployProvidedDependenciesQuery.ignore()
  }

  override def undeploy()(implicit connection: Connection): Unit = {
    Ignore.ignore("DROP VIEW IF EXISTS mvn.dependencies CASCADE")
    Ignore.ignore("DROP VIEW IF EXISTS mvn.provided_dependencies CASCADE")
  }
}
