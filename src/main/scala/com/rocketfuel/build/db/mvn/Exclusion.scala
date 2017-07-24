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

  def byBldIdAndDependencyId()(implicit connection: Connection): Map[Int, Map[Int, Set[Exclusion]]] = {
    for {
      (bldId, bldExclusions) <- list.vector().toSet.groupBy((x: Exclusion) => x.bldId)
    } yield bldId -> bldExclusions.groupBy(_.dependencyId)
  }

  val deploy = Ignore.readTypeResource[Exclusion]("exclusions/create.sql")

  val run = Ignore.readTypeResource[Exclusion]("exclusions/run.sql")

  override def deploy()(implicit connection: Connection): Unit = {
    deploy.ignore()
  }

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mvn.exclusions;")

}
