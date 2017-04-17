package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file.Path
import scala.xml._

case class Model(
  identifier: Model.Identifier,
  repository: Option[String] = None,
  scalaVersion: Option[String] = None,
  javaVersion: Option[String] = None,
  isProto: Boolean,
  configurations: Map[String, Model.Configuration] = Map.empty
) {

  def pom: Elem =
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      {identifier.mavenDefinition}

      <parent>
        {Models.aggregate}
      </parent>

      <dependencies>
        {
          for {
            (configName, config) <- configurations
            dependency <- config.uniqueDependencies
          } yield dependency.mavenDependency(configName)
        }
      </dependencies>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.6.1</version>
            <configuration>
            {
              val j = javaVersion.getOrElse("1.8")
              <source>{j}</source>
              <target>{j}</target>
            }
            </configuration>
          </plugin>
          {
            scalaVersion match {
              case None =>
              case Some(v) =>
                <plugin>
                <groupId>net.alchim31.maven</groupId>
                  <artifactId>scala-maven-plugin</artifactId>
                  <version>3.2.2</version>
                  <executions>
                    <execution>
                      <goals>
                        <goal>compile</goal>
                        <goal>testCompile</goal>
                      </goals>
                    </execution>
                  </executions>
                  <configuration>
                    <scalaVersion>{v}</scalaVersion>
                  </configuration>
                </plugin>
            }

            if (isProto) {
              <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.5.0</version>
                <configuration>
                  <protocExecutable>/usr/local/bin/protoc</protocExecutable>
                </configuration>
                <executions>
                  <execution>
                    <goals>
                      <goal>compile</goal>
                      <goal>test-compile</goal>
                    </goals>
                  </execution>
                </executions>
              </plugin>
            }
          }
        </plugins>
      </build>
    </project>

  def buildSbt: String =
    s"""${identifier.sbtDefinition}
       |
       |libraryDependencies ++= Seq(
       |${
      val depStrings = for {
        (configName, config) <- configurations
        dependency <- config.uniqueDependencies
      } yield dependency.sbtDependency(configName)
      depStrings.mkString(",\n")
    }
       |)
       |
       |${scalaVersion match {case Some(v) => s"scalaVersion := $v" case None => ""}}
       |
       |${if (isProto) "sbtprotobuf.ProtobufPlugin.protobufSettings\nunmanagedResourceDirectories in Compile += (sourceDirectory in sbtprotobuf.ProtobufPlugin.protobufConfig).value" else ""}
     """.stripMargin

  def pluginsSbt: Option[String] = {
    if (isProto)
      Some("""addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.5.4")""")
    else None
  }

}

object Model {

  case class Identifier(
    groupId: String,
    artifactId: String,
    version: String
  ) {
    def mavenDependency(configName: String): Elem =
      <dependency>
        <groupId>{groupId}</groupId>
        <artifactId>{artifactId}</artifactId>
        <version>{version}</version>
        { if (configName != "main") <scope>{configName}</scope> else NodeSeq.Empty}
      </dependency>

    def mavenDefinition: NodeBuffer = {
      <groupId>
        {groupId.drop("java.".length - 1)}
      </groupId>
      <artifactId>
        {artifactId}
      </artifactId>
      <version>
        9.0.0-SNAPSHOT
      </version>
    }

    def sbtDependency(configName: String) =
      s""""$groupId" % "$artifactId" % "$version${if (configName != "main") s""" % "$configName"""" else ""}"""

    def sbtDefinition =
     s"""name := "$artifactId"
        |
        |organization := "${groupId.drop("java.".length - 1)}"
        |
        |version := "$version"
        |"""
  }

  object Identifier {
    val valueOf: PartialFunction[mool.Dependency, Identifier] = {
      case mool.Dependency.Maven(groupId, artifactId, version, scope) =>
        Identifier(groupId, artifactId, version)
    }
  }

  case class Configuration(
    dependencies: Set[Identifier] = Set.empty,
    internalDependencies: Set[Identifier] = Set.empty, //for maven inter-module dependencies
    files: Set[Path] = Set.empty
  ) {
    def uniqueDependencies: Set[Identifier] = {
      for {
        (groupAndArtifact, groupAndArtifcatIds) <- dependencies.groupBy(d => (d.groupId, d.artifactId))
      } yield {
        groupAndArtifcatIds.maxBy(_.version)
      }
    }.toSet
  }

  object Configuration {
    def valueOf(moolModel: mool.Model, dependencies: Set[mool.Dependency]): Configuration = {
      dependencies.toSeq.foldLeft(Configuration()) {
        case (accum, mool.Dependency.Bld(bldPath)) =>
          val bld = moolModel.blds(bldPath)
          val files = bld.srcPaths(moolModel, bldPath)
          accum.copy(files = accum.files ++ files)
        case (accum, mool.Dependency.Maven(groupId, artifactId, version, scope)) =>
          accum.copy(dependencies = accum.dependencies + Identifier(groupId, artifactId, version))
        case (accum, mool.Dependency.RelCfg(moolPath)) =>
          val relCfg = moolModel.relCfgs(moolPath)
          accum.copy(dependencies = accum.dependencies + Identifier(relCfg.group_id, relCfg.artifact_id, "9.0.0-SNAPSHOT"))
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

    val configuration =
      Configuration(
        dependencies = dependencies.toVector.collect(Identifier.valueOf).toSet,
        files = sourcePaths.toSet
      )

    //We might have to make up the identifier.
    val identifier =
      Model.Identifier(
        groupId = path.mkString("."),
        artifactId = path.mkString("."),
        version = moolModel.maxVersion.getOrElse(path, "0.0")
      )

    val configurationName =
      if (bld.rule_type.contains("test")) "test" else "main"

    Model(
      identifier = identifier,
      scalaVersion = bld.scala_version,
      javaVersion = bld.java_version,
      isProto = bld.rule_type == "java_proto_lib",
      repository = bld.maven_specs.map(_.repo_url),
      configurations = Map(configurationName -> configuration)
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
          files = sourcePaths
        )

      val testDependencies =
        testDependenciesOfRelCfg(moolModel)(path)

      val testSourcePaths =
        testDependencies.flatMap {
          case mool.Dependency.Bld(dependencyPath) =>
            val bld = moolModel.blds(dependencyPath)
            bld.srcPaths(moolModel, dependencyPath)
          case _ =>
            Vector.empty
        }

      val testConfiguration =
        Configuration(
          dependencies = testDependencies.collect(Identifier.valueOf),
          files = testSourcePaths
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
        javaVersion = bld.java_version,
        isProto = bld.rule_type == "java_proto_lib",
        repository = bld.maven_specs.map(_.repo_url),
        configurations = Map("main" -> configuration, "test" -> testConfiguration)
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

  def testDependenciesOfRelCfg(moolModel: mool.Model)(relCfgPath: mool.MoolPath): Set[mool.Dependency] = {
    val dependencyPaths = moolModel.relCfgsToBldsTransitive(relCfgPath)
    val testDependencyPaths =
      for {
        dependencyPath <- dependencyPaths
        testDependencyPath <- moolModel.bldsToTestBlds(dependencyPath)
      } yield testDependencyPath

    testDependencyPaths.map(dependencyPath => (dependencyPath, moolModel.blds(dependencyPath))).map((mool.Dependency.ofBld _).tupled)
  }

}
