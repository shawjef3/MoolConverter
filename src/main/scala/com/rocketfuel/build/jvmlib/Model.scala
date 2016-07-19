package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file.Path

case class Model(
  identifier: Model.Identifier,
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

  object Identifier {
    val valueOf: PartialFunction[mool.Dependency, Identifier] = {
        case mool.Dependency.Maven(groupId, artifactId, version) =>
          Identifier(groupId, artifactId, version)
      }
  }

  case class Configuration(
    dependencies: Set[Identifier] = Set.empty,
    files: Map[String, Set[Path]] = Map.empty
  )

  object Configuration {
    def valueOf(moolModel: mool.Model, dependencies: Set[mool.Dependency]): Configuration = {
      val deps =
        for {
          dependency <- dependencies.toVector
        } yield {
          dependency match {
            case mool.Dependency.Bld(bldPath) =>
              val bld = moolModel.blds(bldPath)
              val files = bld.srcPaths(moolModel, bldPath)
              Left(bld.language -> files.toSet)
            case mool.Dependency.Maven(groupId, artifactId, version) =>
              Right(Identifier(groupId, artifactId, version))
            case mool.Dependency.RelCfg(moolPath) =>
              val relCfg = moolModel.relCfgs(moolPath)
              Right(Identifier(relCfg.group_id, relCfg.artifact_id, "0"))
          }
        }
      deps.foldLeft(Configuration()) {
        case (accum, Left((fileConfig, fileName))) =>
          accum.files.get(fileConfig) match {
            case Some(existing) =>
              val updatedFiles =
                accum.files + (fileConfig -> (existing ++ fileName))
              accum.copy(files = updatedFiles)
            case None =>
              accum.copy(files = accum.files + (fileConfig -> fileName))
          }

        case (accum, Right(mvn)) =>
          accum.copy(dependencies = accum.dependencies + mvn)
      }
    }
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
        dependencies = dependencies.collect(Identifier.valueOf),
        files = Map(bld.language -> sourcePaths.toSet)
      )

    val testBldPaths =
      moolModel.bldsToTestBlds(path)

    val testDependencies =
      for {
        testBldPath <- testBldPaths
        dependency <- dependenciesOfBld(moolModel)(testBldPath)
      } yield dependency

    val testSourcePaths =
      for {
        testBldPath <- testBldPaths
        testBld = moolModel.blds(testBldPath)
      } yield {
        val sourcePaths = testBld.srcPaths(moolModel, testBldPath)
        testBld.language -> sourcePaths.toSet
      }

    val testConfiguration =
      Configuration(
        dependencies = testDependencies.collect(Identifier.valueOf),
        files = testSourcePaths.toMap
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
      identifier = identifier.get,
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
      bldPath <- moolModel.relCfgsToBld(path)
    } yield {
      val bld = moolModel.blds(bldPath)
      val dependencies =
        dependenciesOfRelCfg(moolModel)(path)

      val sourcePaths =
        dependencies.flatMap {
          case mool.Dependency.Bld(dependencyPath) =>
            val bld = moolModel.blds(dependencyPath)
            bld.srcPaths(moolModel, dependencyPath)
          case _ =>
            Vector.empty
        }

      val configuration =
        Configuration(
          dependencies = dependencies.collect(Identifier.valueOf),
          files = Map(bld.language -> sourcePaths)
        )

      val identifier =
        Model.Identifier(
          groupId = relCfg.group_id,
          artifactId = relCfg.artifact_id,
          version = relCfg.base_version
        )

      Model(
        identifier = identifier,
        scalaVersion = bld.scala_version,
        repository = bld.maven_specs.map(_.repo_url),
        configurations = Map("main" -> configuration)
      )
    }
  }

  def dependenciesOfBld(moolModel: mool.Model)(path: mool.MoolPath): Set[mool.Dependency] = {
    val bld = moolModel.blds(path)

    for {
      depPath <- bld.compileDepPaths(path)
    } yield {
      val depBld = moolModel.blds(depPath)
      mool.Dependency.ofBld(depPath, depBld)
    }
  } toSet

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
      testPath <- moolModel.bldsToTestBlds(path)
    } yield {
      val testBld = moolModel.blds(testPath)
      mool.Dependency.ofBld(testPath, testBld)
    }
  }

  def dependenciesOfRelCfg(moolModel: mool.Model)(relCfgPath: mool.MoolPath): Set[mool.Dependency] = {
    val myBlds = moolModel.relCfgsToExclusiveBlds(relCfgPath)

    val notMyBldPaths =
      moolModel.relCfgsToBldsTransitive(relCfgPath) -- myBlds

    //For Blds that don't belong to this RelCfg, get the RelCfgs they belong to.
    val notMyBldDependencies: Set[mool.Dependency] =
      for {
        notMyBldPath <- notMyBldPaths
        bld = moolModel.blds(notMyBldPath)
        dependency <- bld.maven_specs match {
          case Some(mavenSpecs) =>
            //Maven dependencies will be pulled in transitively.
            Set.empty[mool.Dependency]
          case None =>
            val relCfgs = moolModel.bldsToRelCfgsTransitive(notMyBldPath)
            if (relCfgs.size > 1)
              assert(false)
            Set[mool.Dependency](mool.Dependency.RelCfg(relCfgs.head))
        }
      } yield dependency

    myBlds.map(mool.Dependency.Bld) ++
      notMyBldDependencies
  }

  def ofDescendState(d: Models.DescendState): Model = {
    val identifier =
      Model.Identifier(
        groupId = d.root.group_id,
        artifactId = d.root.artifact_id,
        version = "0"
      )

    val configurations =
      Map(
        "main" -> Model.Configuration.valueOf(d.model, d.dependencies.main),
        "test" -> Model.Configuration.valueOf(d.model, d.dependencies.test)
      )

    Model(
      identifier = identifier,
      configurations = configurations
    )
  }

}
