package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.{Logger, mool}
import java.nio.file._
import scalaz._, Scalaz._

case class Models(
  models: Map[mool.MoolPath, Model],
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

}

object Models
  extends Logger {

  def ofMoolRepository(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty).resolveConflicts
    val models = ofMoolRelCfgs(moolModel)
    Models(
      models = models,
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

  def testBlds(moolModel: mool.Model)(path: mool.MoolPath): Map[mool.MoolPath, mool.Bld] = {
    moolModel.blds.filter(_._2.rule_type.contains("test"))
  }

}
