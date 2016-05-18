package com.rocketfuel.jvmlib

import com.rocketfuel.mool

case class Model(
  groupId: String,
  artifactId: String,
  version: String,
  scalaVersion: Option[String],
  dependencies: Vector[Model.Dependency]
)

object Model {

  sealed trait Dependency

  object Dependency {
    case class Local(
      path: Vector[String],
      name: String
    ) extends Dependency

    case class Remote(  groupId: String,
      artifactId: String,
      version: String
    ) extends Dependency
  }

  def ofMoolProject(
    moolModel: mool.Model
  )(path: Vector[String],
    name: String
  )(bld: mool.Bld
  ): Option[Model] = {
    for {
      pathRelCfgs <- moolModel.relCfgs.get(path)
      relCfg <- pathRelCfgs.get(name)
    } yield {
      val dependencies =
        for {
          deps <- bld.deps.toVector
          dep <- deps
        } yield {
          val depParts =
            if (dep.startsWith(".")) (path :+ dep.drop(1)).toVector
            else {
              //drop the leading "mool"
              dep.split('.').drop(1).toVector
            }
          val depPath = depParts.dropRight(1)
          val depName = depParts.last
          val depPathBlds = moolModel.blds(depPath)
          val depBld = depPathBlds(depName)

          depBld.maven_specs match {
            case Some(mavenSpecs) =>
              Dependency.Remote(
                groupId = mavenSpecs.group_id,
                artifactId = mavenSpecs.artifact_id,
                version = mavenSpecs.version
              )
            case None =>
              Dependency.Local(
                path = depPath,
                name = depName
              )
          }

        }

      Model(
        groupId = relCfg.group_id,
        artifactId = relCfg.artifact_id,
        version = relCfg.base_version,
        scalaVersion = bld.scala_version,
        dependencies = dependencies.toVector
      )
    }
  }

  def ofMoolModel(model: mool.Model): Iterable[Model] = {
    for {
      (path, blds) <- model.blds
      (name, bld) <- blds
      model <- ofMoolProject(model)(path, name, bld)
    } yield model
  }
}
