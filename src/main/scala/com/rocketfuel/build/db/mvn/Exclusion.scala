package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._
import scala.xml.Elem

case class Exclusion(
  id: Int,
  bldId: Int,
  dependencyId: Int,
  excludedGroupId: String,
  excludedArtifactId: String
) {

  lazy val mavenDefinition: Elem =
    <exclusion>
      <groupId>{excludedGroupId}</groupId>
      <artifactId>{excludedArtifactId}</artifactId>
    </exclusion>

}

object Exclusion extends Deployable {

  val list =
    Select[Exclusion](
      """SELECT
        |  id,
        |  bld_id bldId,
        |  dependency_id dependencyId,
        |  excluded_group_id excludedGroupId,
        |  excluded_artifact_id excludedArtifactId
        |FROM mvn.exclusions
        |""".stripMargin
    )

  def byBldId()(implicit connection: Connection): Map[Int, Set[Exclusion]] = {
    list.vector().toSet.groupBy(_.bldId)
  }

  def run()(implicit connection: Connection): Unit = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream("exclusions.sql")).mkString
    Ignore.ignore(source)
  }

  override def deploy()(implicit connection: Connection): Unit = {
    Ignore.ignore(
      """CREATE TABLE IF NOT EXISTS mvn.exclusions (
        |  id serial PRIMARY KEY,
        |  bld_id int NOT NULL,
        |  dependency_id int NOT NULL,
        |  excluded_group_id text NOT NULL,
        |  excluded_artifact_id text NOT NULL
        |);
        |""".stripMargin
    )
  }

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mvn.exclusions;")

}
