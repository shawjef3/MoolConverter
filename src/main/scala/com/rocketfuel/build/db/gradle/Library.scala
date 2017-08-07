package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Library(id: String, path: String, rule_type: String, scala_version: Option[String],
                      java_version: Option[String], group_id: Option[String], artifact_id: Option[String],
                      version: Option[String], repo_url: Option[String], classifier: Option[String]) {
  def isMavenDep() =
    (path.startsWith("java.mvn.") || path.startsWith("java.com.rocketfuel.ei.datamon")) &&
      group_id.isDefined && artifact_id.isDefined && version.isDefined
}

object Library extends Deployable with Logger {
  val list = Select[Library]("SELECT * FROM gradle.libraries")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "libraries.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.libraries CASCADE")

  def libReference(libPath: String, quoted: Boolean = true) = {
    libPath.stripPrefix("java.mvn.")
  }

  implicit def orderingByName[A <: Library]: Ordering[A] =
    Ordering.by(l => (l.path))
}
