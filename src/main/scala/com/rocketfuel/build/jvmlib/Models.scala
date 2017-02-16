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
      (srcLanguage, files) <- conf.files
      file <- files
    } yield {
      val relative = moolRoot.relativize(file)
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount)

      val destinationFile =
        projectRoot.resolve(confName).
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
      kvp <- Map(pom -> model.pom.toString, sbt -> model.sbt)
    } yield kvp
  } toMap

  def aggregatePom: Elem =
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      <groupId>com.rocketfuel</groupId>
      <artifactId>aggregate</artifactId>
      <version>9.0.0-SNAPSHOT</version>
      <packaging>pom</packaging>

      <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <jdkName>JDK 1.8</jdkName>
        <compileSource>1.8</compileSource>
        <JAVAC>${{env.JAVA_HOME}}/bin/javac</JAVAC>
        <JAVA>${{env.JAVA_HOME}}/bin/java</JAVA>
      </properties>

      <modules>
        {
          for ((relCfgPath, _) <- models) yield <module>{relCfgPath.last}</module>
        }
      </modules>
    </project>

  val toIdentifier: String => String = {
    case "" => ""
    case x =>
      val head =
        if (x.head.isUnicodeIdentifierStart) x
        else '_'
      val tail = head + x.tail.map {
        case c if c.isUnicodeIdentifierPart => c
        case _ => '_'
      }
      head + tail
  }

  def aggregateSbt: String = {
    val projects =
      for ((relCfgPath, _) <- models) yield {
        "lazy val " + toIdentifier(relCfgPath.mkString) + " = project.in(file(" + relCfgPath.mkString(".") + "))" //todo: dependsOn
      }

    s"""lazy val root =
       |  project.in(file(".")).aggregate(${models.keys.map(_.mkString).mkString("\n", ",\n    ", "\n  ")})
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

}
