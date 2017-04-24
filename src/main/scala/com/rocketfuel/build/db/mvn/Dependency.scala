package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._
import scala.xml._

case class Dependency(
  sourceId: Int,
  targetId: Int,
  groupId: String,
  artifactId: String,
  version: String,
  scope: String
) {

  lazy val mavenDefinition: NodeSeq =
    <dependency>
      <groupId>
        {groupId}
      </groupId>
      <artifactId>
        {artifactId}
      </artifactId>
      <version>
        {version}
      </version>
      <scope>
        {scope}
      </scope>
    </dependency>

}

object Dependency extends Deployable {
  val list =
    Select[Dependency]("SELECT source_id sourceId, target_id targetId, group_id groupId, artifact_id artifactId, version, scope FROM mvn.dependencies")

  val selectBySourceId =
    Select[Dependency](list.queryText + " WHERE source_id = @sourceId")

  val deployQuery = Ignore.readClassResource(classOf[Dependency], "dependencies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.dependencies CASCADE")
}
