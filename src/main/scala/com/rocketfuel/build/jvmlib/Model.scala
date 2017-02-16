package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file.Path
import scala.xml.{Elem, NodeBuffer}

case class Model(
  identifier: Model.Identifier,
  repository: Option[String] = None,
  scalaVersion: Option[String] = None,
  configurations: Map[String, Model.Configuration] = Map.empty
) {

  def pom: Elem =
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      {identifier.mavenDefinition}

      <dependencies>
        {
          for {
            (configName, config) <- configurations
            dependency <- config.dependencies
          } yield dependency._1.mavenDependency
        }
      </dependencies>

    </project>

  def sbt: String =
    s"""${identifier.sbtDefinition}
       |
       |libraryDependencies ++= Seq(
       |${
      val depStrings = for {
        (configName, config) <- configurations
        dependency <- config.dependencies
      } yield dependency._1.sbtDependency
      depStrings.mkString(",\n")
    }
       |)
     """.stripMargin

}

object Model {

  case class Identifier(
    groupId: String,
    artifactId: String,
    version: String
  ) {
    def mavenDependency: Elem =
      <dependency>
        <groupId>{groupId}</groupId>
        <artifactId>{artifactId}</artifactId>
        <version>{version}</version>
      </dependency>

    def mavenDefinition: NodeBuffer = {
      <groupId>
        {groupId.split('.').tail.mkString(".")}
      </groupId>
      <artifactId>
        {artifactId}
      </artifactId>
      <version>
        {version}
      </version>
    }

    def sbtDependency =
      s""""$groupId" % "$artifactId" % "$version""""

    def sbtDefinition =
     s"""name := "$artifactId"
        |
        |organization := "${groupId.split('.').tail.mkString(".")}"
        |
        |version := "$version"
        |"""
  }

  object Identifier {
    val valueOf: PartialFunction[mool.Dependency, (Identifier, String)] = {
      case mool.Dependency.Maven(groupId, artifactId, version) =>
        Identifier(groupId, artifactId, version) -> "main"
    }
  }

  case class Configuration(
    dependencies: Map[Identifier, String] = Map.empty,
    files: Map[String, Set[Path]] = Map.empty
  )

  object Configuration {
    def valueOf(moolModel: mool.Model, dependencies: Set[mool.Dependency]): Configuration = {
      val deps =
        for {
          dependency <- dependencies.toSeq
        } yield {
          dependency match {
            case mool.Dependency.Bld(bldPath) =>
              val bld = moolModel.blds(bldPath)
              val files = bld.srcPaths(moolModel, bldPath)
              Left(bld.language -> files.toSet)
            case mool.Dependency.Maven(groupId, artifactId, version) =>
              Right(Identifier(groupId, artifactId, version) -> "main")
            case mool.Dependency.RelCfg(moolPath) =>
              val relCfg = moolModel.relCfgs(moolPath)
              Right(Identifier(relCfg.group_id, relCfg.artifact_id, "0") -> "main")
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
        dependencies = dependencies.toVector.collect(Identifier.valueOf).toMap,
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
        dependencies = testDependencies.toVector.collect(Identifier.valueOf).toMap,
        files = testSourcePaths.toMap
      )

    val identifier =
      for {
        relCfg <- moolModel.relCfgs.get(path)
      } yield {
        Model.Identifier(
          groupId = relCfg.group_id,
          artifactId = relCfg.artifact_id,
          version = moolModel.maxVersion.getOrElse(path, relCfg.base_version)
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
          dependencies = dependencies.collect(Identifier.valueOf).toMap,
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
    val dependencyPaths = moolModel.relCfgsToBldsTransitive(relCfgPath)

    dependencyPaths.map(dependencyPath => (dependencyPath, moolModel.blds(dependencyPath))).map((mool.Dependency.ofBld _).tupled)
  }

}
