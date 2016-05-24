package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import com.rocketfuel.mool.RelCfg
import java.nio.file.{Files, Path}

case class Model(
  groupId: String,
  artifactId: String,
  version: String,
  repository: Option[String],
  scalaVersion: Option[String],
  dependencies: Set[Model.Dependency],
  files: Vector[Path]
) {
  def pathsToCopy(originRoot: Path, destinationRoot: Path): Vector[(Path, Path)] = {
    for (file <- files) yield {
      val relative = originRoot.relativize(file)
      val destinationFile = destinationRoot.resolve(relative)
      Files.createDirectories(destinationFile)
      (file, destinationFile)
    }
  }
}

object Model {

  sealed trait Dependency

  object Dependency {
    case class Local(
      path: Vector[String]
    ) extends Dependency

    case class Remote(
      groupId: String,
      artifactId: String,
      version: String
    ) extends Dependency
  }

  def ofMoolBld(
    moolModel: mool.Model
  )(path: Vector[String],
    bld: mool.Bld
  ): Option[Model] = {
    val dependencies =
      dependenciesOfBld(moolModel)(path)

    val sourcePaths =
      sourcePathsOfBld(moolModel)(path, bld)

    for {
      relCfg <- moolModel.relCfgs.get(path)
    } yield {
      Model(
        groupId = relCfg.group_id,
        artifactId = relCfg.artifact_id,
        version = relCfg.base_version,
        scalaVersion = bld.scala_version,
        dependencies = dependencies,
        repository = bld.maven_specs.map(_.repo_url),
        files = sourcePaths
      )
    }
  }

  /**
    * Create a Model for each BLD.
    *
    * @param model
    * @return
    */
  def ofMoolBlds(model: mool.Model): Map[Vector[String], Model] = {
    for {
      (path, bld) <- model.blds
      model <- ofMoolBld(model)(path, bld)
    } yield path -> model
  }

  def ofMoolRelCfg(
    moolModel: mool.Model
  )(path: Vector[String],
    relCfg: RelCfg
  ): Option[Model] = {
    for {
      withDeps <- relCfg.`jar-with-dependencies`
      targetBldParts = withDeps.target.split('.').toVector
      if targetBldParts.startsWith(Vector("mool", "java"))
    } yield {

      val targetBldPath = targetBldParts.drop(1)
      val bld = moolModel.blds(targetBldPath)

      val dependencies =
        dependenciesOfBld(moolModel)(targetBldPath)

      val sourcePaths =
        sourcePathsOfBld(moolModel)(targetBldPath, bld)

      Model(
        groupId = relCfg.group_id,
        artifactId = relCfg.artifact_id,
        version = relCfg.base_version,
        scalaVersion = bld.scala_version,
        dependencies = dependencies,
        repository = bld.maven_specs.map(_.repo_url),
        files = sourcePaths
      )
    }
  }

  /**
    * Create a Model for each RelCfg.
    *
    * @param model
    * @return
    */
  def ofMoolRelCfgs(model: mool.Model): Map[Vector[String], Model] = {
    for {
      (path, relCfg) <- model.relCfgs
      model <- ofMoolRelCfg(model)(path, relCfg)
    } yield path -> model
  }

  def dependenciesOfBld(moolModel: mool.Model)(path: Vector[String]): Set[Dependency] = {
    for {
      depPath <- moolModel.bldsToBlds(path)
    } yield {
      val depBld = moolModel.blds(depPath)

      depBld.maven_specs match {
        case Some(mavenSpecs) =>
          Dependency.Remote(
            groupId = mavenSpecs.group_id,
            artifactId = mavenSpecs.artifact_id,
            version = mavenSpecs.version
          )
        case None =>
          Dependency.Local(depPath)
      }
    }
  }

  def sourcePathsOfBld(moolModel: mool.Model)(path: Vector[String], bld: mool.Bld): Vector[Path] = {
    bld.srcPaths(moolModel, path)
  }

  /**
    * Get the source dependencies of a relcfg. Look in all its dependencies transitively.
    *
    * @param moolModel
    * @param path
    * @param bld
    * @return
    */
//  def transitiveSourcePathsOfBld(moolModel: mool.Model)(path: Vector[String], name: String, bld: mool.Bld): Set[Path] = {
//    for {
//      dependency <- bld.deps.toVector.flatten
//    } yield {
//      val dependencyPath = dependency.pathParts
//      val dependencyName = dependency.pathName
//      if (moolModel.relCfgs.contains(dependencyPath) && moolModel.relCfgs(dependencyPath).contains(dependencyName)) Set.empty
//      else
//    }
//  }

}
