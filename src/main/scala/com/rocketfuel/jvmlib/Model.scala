package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import com.rocketfuel.mool.RelCfg
import java.nio.file.{Files, Path}

case class Model(
  identifier: Option[Model.Identifier],
  repository: Option[String],
  scalaVersion: Option[String],
  configurations: Map[String, Model.Configuration]
) {
  def pathsToCopy(originRoot: Path, destinationRoot: Path): Map[Path, Path] = {
    val destinationSrc = destinationRoot.resolve("src")
    for {
      (configName, config) <- configurations
      configDestination = destinationSrc.resolve(configName)
      file <- config.files
    } yield {
      val relative = originRoot.relativize(file)
      //Remove the leading "java"
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount - 2)
      val destinationFile = configDestination.resolve(relativeWithoutJava)
      Files.createDirectories(destinationFile)
      (file, destinationFile)
    }
  }
}

object Model {

  case class Identifier(
    groupId: String,
    artifactId: String,
    version: String
  )

  case class Configuration(
    dependencies: Set[Dependency],
    files: Set[Path]
  )

  sealed trait Dependency

  object Dependency {
    def of(bldPath: mool.MoolPath, bld: mool.Bld): Dependency = {
      bld.maven_specs match {
        case Some(mavenSpecs) =>
          Dependency.Remote(
            groupId = mavenSpecs.group_id,
            artifactId = mavenSpecs.artifact_id,
            version = mavenSpecs.version
          )
        case None =>
          Dependency.Local(bldPath)
      }
    }

    case class Local(
      path: Vector[String]
    ) extends Dependency

    case class Remote(
      groupId: String,
      artifactId: String,
      version: String
    ) extends Dependency
  }

  /**
    * Create a Model from one Mool BLD.
    */
  def ofMoolBld(
    moolModel: mool.Model
  )(path: Vector[String],
    bld: mool.Bld
  ): Model = {
    val dependencies =
      dependenciesOfBld(moolModel)(path)

    val sourcePaths =
      bld.srcPaths(moolModel, path)

    val mainConfiguration =
      Configuration(
        dependencies = dependencies,
        files = sourcePaths.toSet
      )

    val testBldPaths =
      moolModel.testBlds.getOrElse(path, Set.empty)

    val testDependencies =
      for {
        testBldPath <- testBldPaths
        dependency <- dependenciesOfBld(moolModel)(testBldPath)
      } yield dependency

    val testSourcePaths =
      for {
        testBldPath <- testBldPaths
        testBld = moolModel.blds(testBldPath)
        sourcePath <- testBld.srcPaths(moolModel, testBldPath)
      } yield sourcePath

    val testConfiguration =
      Configuration(
        dependencies = testDependencies,
        files = testSourcePaths
      )

    val identifier =
      for {
        relCfg <- moolModel.relCfgs.get(path)
      } yield {
        Model.Identifier(
          groupId = relCfg.group_id,
          artifactId = relCfg.artifact_id,
          version = relCfg.base_version
        )
      }

    Model(
      identifier = identifier,
      scalaVersion = bld.scala_version,
      repository = bld.maven_specs.map(_.repo_url),
      configurations = Map("main" -> mainConfiguration, "test" -> testConfiguration)
    )
  }

  /**
    * Create a Model for each BLD.
    *
    * @param model
    * @return
    */
  def ofMoolBlds(model: mool.Model): Map[mool.MoolPath, Model] = {
    for {
      (path, bld) <- model.blds
    } yield path -> ofMoolBld(model)(path, bld)
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
        bld.srcPaths(moolModel, targetBldPath)

      val configuration =
        Configuration(
          dependencies = dependencies,
          files = sourcePaths.toSet
        )

      val testBldPaths =
        moolModel.testBlds.getOrElse(targetBldPath, Set.empty)

      val testDependencies =
        for {
          testBldPath <- testBldPaths
          dependency <- dependenciesOfBld(moolModel)(testBldPath)
        } yield dependency

      val testSourcePaths =
        for {
          testBldPath <- testBldPaths
          testBld = moolModel.blds(testBldPath)
          sourcePath <- testBld.srcPaths(moolModel, testBldPath)
        } yield sourcePath

      val testConfiguration =
        Configuration(
          dependencies = testDependencies,
          files = testSourcePaths
        )

      val identifier =
        Model.Identifier(
          groupId = relCfg.group_id,
          artifactId = relCfg.artifact_id,
          version = relCfg.base_version
        )

      Model(
        identifier = Some(identifier),
        scalaVersion = bld.scala_version,
        repository = bld.maven_specs.map(_.repo_url),
        configurations = Map("main" -> configuration, "test" -> testConfiguration)
      )
    }
  }

  /**
    * Create a Model for each RelCfg.
    *
    * @param model
    * @return
    */
  def ofMoolRelCfgs(model: mool.Model): Map[mool.MoolPath, Model] = {
    for {
      (path, relCfg) <- model.relCfgs
      model <- ofMoolRelCfg(model)(path, relCfg)
    } yield path -> model
  }

  def testBlds(moolModel: mool.Model)(path: mool.MoolPath): Map[mool.MoolPath, mool.Bld] = {
    moolModel.blds.filter {
      case (bldPath, bld) =>
        bld.rule_type.contains("test") &&
          moolModel.bldsToBlds.get(bldPath).exists(_.contains(path))
    }
  }

  def dependenciesOfBld(moolModel: mool.Model)(path: mool.MoolPath): Set[Dependency] = {
    for {
      depPath <- moolModel.bldToBldsTransitive(path)
    } yield {
      val depBld = moolModel.blds(depPath)
      Dependency.of(depPath, depBld)
    }
  }

  def testsOfBld(moolModel: mool.Model)(path: mool.MoolPath): Set[Dependency] = {
    for {
      testPath <- moolModel.testBlds(path)
    } yield {
      val testBld = moolModel.blds(testPath)
      Dependency.of(testPath, testBld)
    }
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
