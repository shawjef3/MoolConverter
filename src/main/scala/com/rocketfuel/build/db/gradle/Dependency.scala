package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Dependency(prj_id: String, prj_path: String,
                      id: String, path: String, rule_type: String, scala_version: String,
                      java_version: String, group_id: String, artifact_id: String,
                      version: String, repo_url: String, classifier: String, is_compile: Boolean)

object Dependency extends Deployable with Logger {
  val list = Select[Dependency]("SELECT * FROM gradle.dependencies")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "dependencies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.dependencies CASCADE")
}
