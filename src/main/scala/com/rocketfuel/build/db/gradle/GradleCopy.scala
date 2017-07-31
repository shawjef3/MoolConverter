package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.build.db.mvn.{Copy, Identifier}
import com.rocketfuel.sdbc.PostgreSql._

case class GradleCopy(source: String,
                      bldPath: Seq[String],
                      packagePath: String,
                      destination: String)

object GradleCopy extends Deployable {
  val all =
    Select[GradleCopy]("SELECT source, bld_path AS bldPath, package_path AS packagePath, destination FROM gradle.copies")

  def toCopy(gc: GradleCopy): Copy = {
    Copy(source = gc.source,
      packagePath = gc.packagePath,
      destination = Projects.pathToModulePath(gc.bldPath) + "/" + gc.destination)
  }

  val deployQuery = Ignore.readClassResource(GradleCopy.getClass, "gradle_copies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.copies CASCADE")

}