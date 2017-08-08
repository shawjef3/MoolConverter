package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.{Copy, Deployable}
import com.rocketfuel.sdbc.PostgreSql._

object MavenCopy extends Deployable {
  val all =
    Select[Copy]("SELECT source, package_path AS packagePath, destination FROM mvn.copies")

  val deployQuery = Ignore.readClassResource(getClass, "copies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.copies CASCADE")

}
