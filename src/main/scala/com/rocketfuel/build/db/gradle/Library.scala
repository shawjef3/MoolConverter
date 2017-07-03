package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Library(id: String, path: String, rule_type: String, scala_version: String,
                      java_version: String, group_id: String, artifact_id: String,
                      version: String, repo_url: String, classifier: String)

object Library extends Deployable with Logger {
  val list = Select[Library]("SELECT * FROM gradle.dependencies")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "libraries.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.libraries CASCADE")
}
