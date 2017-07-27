package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class IgnoredBlds(id: Int, path: String)

object IgnoredBlds extends Deployable with Logger {
  val list = Select[IgnoredBlds]("SELECT * FROM gradle.ignored_blds")

  val deployQuery = Ignore.readClassResource(classOf[IgnoredBlds], "ignored_deps.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.ignored_blds CASCADE")

  implicit def orderingByName[A <: IgnoredBlds]: Ordering[A] =
    Ordering.by(l => (l.path))
}
