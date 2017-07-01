package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class ProjectMapping(prj_path: String, bld_path: String)

object ProjectMapping extends Deployable {
  val list = Select[ProjectMapping]("SELECT * FROM gradle.project_mapping")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "project_mapping.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.project_mapping CASCADE")
}
