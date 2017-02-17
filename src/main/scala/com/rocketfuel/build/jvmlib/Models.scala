package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.{Logger, mool}
import java.nio.file._
import scalaz._
import Scalaz._
import scala.xml.Elem

case class Models(
  models: Map[mool.MoolPath, Model],
  modelDependencies: Map[mool.MoolPath, Set[mool.MoolPath]],
  moolModel: mool.Model,
  moolRoot: Path
) {

  def copies(targetPath: Path): Map[Path, Path] = {
    for {
      (relCfgPath, model) <- models
      projectRoot = targetPath.resolve(relCfgPath.last)
      (confName, conf) <- model.configurations
      file <- conf.files
    } yield {
      val relative = moolRoot.relativize(file)
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount)
      val srcLanguage = {
        file.getFileName.toString.split('.').last match {
          case language@("proto" | "java" | "scala") =>
            language
          case "yml" | "txt" | "json" | "properties" | "ser" =>
            "resources"
        }
      }

      val destinationFile =
        projectRoot.
          resolve("src").
          resolve(confName).
          resolve(srcLanguage).
          resolve(relativeWithoutJava)

      file -> destinationFile
    }
  }

  def copy(targetPath: Path): Unit = {
    for ((src, dst) <- copies(targetPath)) {
      Files.createDirectories(dst.getParent)
      if (!Files.exists(dst))
        Files.copy(src, dst)
    }
  }

  def projectFiles(targetPath: Path): Map[Path, String] = {
    for {
      (relCfgPath, model) <- models
      projectRoot = targetPath.resolve(relCfgPath.last)
      pom = projectRoot.resolve("pom.xml")
      sbt = projectRoot.resolve("build.sbt")
      buildProperties = projectRoot.resolve("project").resolve("build.properties")
      kvp <- Map(pom -> model.pom.toString, sbt -> model.buildSbt, buildProperties -> "0.13.13")
    } yield kvp
  } toMap

  def aggregatePom: Elem =
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      {Models.aggregate}
      <packaging>pom</packaging>

      <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
      </properties>

      <modules>
        {
          for ((relCfgPath, _) <- models) yield <module>{relCfgPath.last}</module>
        }
      </modules>
    </project>

  val toIdentifier: String => String = {
    ident =>
      if (!ident.head.isUnicodeIdentifierStart || !ident.tail.forall(_.isUnicodeIdentifierPart))
        "`" + ident + "`"
      else ident
  }

  def aggregateSbt: String = {
    val projects =
      for ((relCfgPath, _) <- models) yield {
        "lazy val " + toIdentifier(relCfgPath.last) + " = project.in(file(\"" + relCfgPath.last + "\"))" //todo: dependsOn
      }

    s"""lazy val root =
       |  project.in(file(".")).aggregate(${models.keys.map(key => toIdentifier(key.last)).mkString("\n", ",\n    ", "\n  ")})
       |
       |${projects.mkString("\n")}
     """.stripMargin
  }

}

object Models
  extends Logger {

  def ofMoolRepository(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty).resolveConflicts
    val models = ofMoolRelCfgs(moolModel)
//    val modelDependencies = dependenciesOfModel(moolModel)
    Models(
      models = models,
      modelDependencies = Map.empty,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }

  /**
    * Create a Model for each RelCfg.
    */
  def ofMoolRelCfgs(model: mool.Model): Map[mool.MoolPath, Model] = {
    for {
      (path, relCfg) <- model.relCfgs
      model <- Model.ofMoolRelCfg(model)(path, relCfg)
    } yield path -> model
  }

//  def dependenciesOfModel(model: mool.Model): Map[mool.MoolPath, Set[mool.MoolPath]] = {
//    for {
//      (relCfgPath, relCfg) <- model.relCfgs
//    }
//  }

  def testBlds(moolModel: mool.Model)(path: mool.MoolPath): Map[mool.MoolPath, mool.Bld] = {
    moolModel.blds.filter(_._2.rule_type.contains("test"))
  }

  val aggregate = {
    <groupId>com.rocketfuel</groupId>
    <artifactId>aggregate</artifactId>
    <version>9.0.0-SNAPSHOT</version>
  }

}
