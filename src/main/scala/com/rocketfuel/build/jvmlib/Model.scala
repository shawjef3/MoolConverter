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

  case class Configuration(
    dependencies: Set[Identifier],
    files: Map[String, Set[Path]]
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

    val srcLanguage =
      bld.rule_type match {
        case "file_coll" =>
          "resources"
        case "java_proto_lib" =>
          "proto"
        case "java_lib" | "java_test" =>
          "java"
        case "scala_lib" | "scala_test" =>
          "scala"
      }

    val mainConfiguration =
      Configuration(
        dependencies = dependencies,
        files = Map(srcLanguage -> sourcePaths.toSet)
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
        val srcLanguage =
          testBld.rule_type match {
            case "file_coll" =>
              "resources"
            case "java_proto_lib" =>
              "proto"
            case "java_lib" | "java_test" =>
              "java"
            case "scala_lib" | "scala_test" =>
              "scala"
          }
        val sourcePaths = testBld.srcPaths(moolModel, testBldPath)
        srcLanguage -> sourcePaths.toSet
      }

    val testConfiguration =
      Configuration(
        dependencies = testDependencies,
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

      val srcLanguage =
        bld.rule_type match {
          case "file_coll" =>
            "resources"
          case "java_proto_lib" =>
            "proto"
          case "java_lib" | "java_test" =>
            "java"
          case "scala_lib" | "scala_test" =>
            "scala"
        }

      val configuration =
        Configuration(
          dependencies = dependencies.filter(_.isInstanceOf[mool.Dependency.Maven]),
          files = Map(srcLanguage -> sourcePaths)
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
        configurations = Map("main" -> configuration)
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
            assert(relCfgs.size <= 1)
            Set[mool.Dependency](mool.Dependency.RelCfg(relCfgs.head))
        }
      } yield dependency

    myBlds.map(mool.Dependency.Bld) ++
      notMyBldDependencies
  }

  def ofDescendState(d: mool.Dependency.DescendState): Model = {
    val identifier =
      Model.Identifier(
        groupId = d.root.group_id,
        artifactId = d.root.artifact_id,
        version = d.root.base_version
      )

    Model(
      identifier = Some(identifier),
      configurations =
        Map(
          "main" -> Model.Configuration(d.dependencies.main, d.sources.main),
          "test" -> Model.Configuration(d.dependencies.test, d.sources.test)
        )
    )
  }

}
