package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Dependency(prj_path: String,
                      id: Int, path: String, rule_type: String, scala_version: Option[String],
                      java_version: Option[String], group_id: Option[String], artifact_id: Option[String],
                      version: Option[String], repo_url: Option[String], classifier: Option[String],
                      is_compile: Boolean) {

  def isMavenDep() =
    path.startsWith("java.mvn.") && group_id.isDefined && artifact_id.isDefined && version.isDefined
}

object Dependency extends Deployable with Logger {
  val list = Select[Dependency]("SELECT * FROM gradle.dependencies")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "dependencies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.dependencies CASCADE")
}
