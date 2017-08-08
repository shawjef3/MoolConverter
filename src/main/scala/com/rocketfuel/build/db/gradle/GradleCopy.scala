package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.{Copy, Deployable}
import com.rocketfuel.sdbc.PostgreSql._

object GradleCopy extends Deployable {
  val all =
    Select[Copy]("SELECT source, bld_path AS bldPath, package_path AS packagePath, destination FROM gradle.copies")

  val deployQuery = Ignore.readClassResource(getClass, "gradle_copies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.copies CASCADE")

}
