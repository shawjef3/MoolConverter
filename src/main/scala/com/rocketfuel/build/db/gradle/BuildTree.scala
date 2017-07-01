package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.{Deployable, SelectableById}
import com.rocketfuel.sdbc.PostgreSql._

object BuildTree extends Deployable {
  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "build_tree.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.build_tree CASCADE")
}
