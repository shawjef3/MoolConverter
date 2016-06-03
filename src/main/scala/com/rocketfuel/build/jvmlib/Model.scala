package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file.Path

case class Model(
  identifier: Option[Model.Identifier] = None,
  repository: Option[String] = None,
  scalaVersion: Option[String] = None,
  configurations: Map[String, Model.Configuration] = Map.empty
)

object Model {

  case class Identifier(
    groupId: String,
    artifactId: String,
    version: String
  )

  case class Configuration(
    dependencies: Set[mool.Dependency],
    files: Set[Path]
  )

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
      moolModel.bldToTestBldsTransitive.getOrElse(path, Set.empty)

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
    relCfg: mool.RelCfg
  ): Option[Model] = {
    for {
      withDeps <- relCfg.`jar-with-dependencies`
      targetBldParts = withDeps.target.split('.').toVector
      if targetBldParts.startsWith(Vector("mool", "java"))
    } yield {
      //Drop the leading "mool" and get the bld.
      val targetBldPath = targetBldParts.drop(1)
      val bld = moolModel.blds(targetBldPath)

      val dependencies =
        dependenciesOfRelCfg(moolModel)(path)

      val dependencySourcePaths =
        dependencies.toVector.flatMap {
          case mool.Dependency.Bld(path) =>
            val bld = moolModel.blds(path)
            bld.srcPaths(moolModel, path)
          case _ =>
            Vector.empty
        }

      val sourcePaths =
        bld.srcPaths(moolModel, targetBldPath) ++
          dependencySourcePaths

      val configuration =
        Configuration(
          dependencies = dependencies.filter(_.isInstanceOf[mool.Dependency.Maven]),
          files = sourcePaths.toSet
        )

      // Get paths to the test BLDs that depend on the current BLD
      // and its dependencies.
      val testBldPaths =
        for {
          dependency <- moolModel.bldsToBldsTransitive(targetBldPath) + targetBldPath
          testDependency <- moolModel.bldToTestBldsTransitive.getOrElse(dependency, Set.empty)
        } yield testDependency

      //Get all the remote dependencies for all the test BLDs.
      val testRemoteDependencies =
        for {
          testBldPath <- testBldPaths
          dependency <- dependenciesOfBld(moolModel)(testBldPath)
          if dependency.isInstanceOf[mool.Dependency.Maven]
        } yield dependency

      val testSourcePaths =
        for {
          testBldPath <- testBldPaths
          bld = moolModel.blds(testBldPath)
          src <- bld.srcPaths(moolModel, testBldPath)
        } yield src

      val testConfiguration =
        Configuration(
          dependencies = testRemoteDependencies,
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

  def dependenciesOfBld(moolModel: mool.Model)(path: mool.MoolPath): Set[mool.Dependency] = {
    for {
      depPath <- moolModel.bldsToBldsTransitive(path)
    } yield {
      val depBld = moolModel.blds(depPath)
      mool.Dependency.ofBld(depPath, depBld)
    }
  }

  def dependenciesOfRelCfg(moolModel: mool.Model)(relCfgPath: mool.MoolPath): Set[mool.Dependency] = {
    val relCfgBlds = moolModel.relCfgsToBldsTransitive(relCfgPath)
    val myBlds =
      relCfgBlds.filter { bldPath =>
        val bldRelCfgs = moolModel.bldsToRelCfgs(bldPath)
        bldRelCfgs == Set(relCfgPath)
      }

    val notMyBlds =
      relCfgBlds -- myBlds

    //For Blds that don't belong to this RelCfg, get the RelCfgs they belong to.
    val notMyBldRelCfgs: Set[mool.Dependency] =
      for {
        notMyBld <- notMyBlds
        relCfgs = moolModel.bldsToRelCfgs(notMyBld)
        () = assert(relCfgs.size == 1)
        dependency: mool.Dependency <- mool.Dependency.ofBldOrOwner(moolModel, relCfgs.head, notMyBld).toSet
      } yield dependency

    val remoteDependencies: Set[mool.Dependency] =
      for {
        myBldPath <- myBlds
        myBld = moolModel.blds(myBldPath)
        mavenSpecs: mool.Bld.MavenSpecs <- myBld.maven_specs.toSet
      } yield mool.Dependency.Maven(mavenSpecs.group_id, mavenSpecs.artifact_id, mavenSpecs.version)

    notMyBldRelCfgs ++ remoteDependencies
  }

  def testDependenciesOfBld(moolModel: mool.Model)(path: mool.MoolPath): Set[mool.Dependency] = {
    for {
      depPath <- moolModel.bldsToBldsTransitive(path)
      depBld = moolModel.blds(depPath)
      if depBld.rule_type.contains("test")
    } yield {
      mool.Dependency.ofBld(depPath, depBld)
    }
  }

  def testsOfBld(moolModel: mool.Model)(path: mool.MoolPath): Set[mool.Dependency] = {
    for {
      testPath <- moolModel.bldToTestBlds(path)
    } yield {
      val testBld = moolModel.blds(testPath)
      mool.Dependency.ofBld(testPath, testBld)
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
