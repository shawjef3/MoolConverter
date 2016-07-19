package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file.{Path, Paths}

case class Models(
  models: Map[mool.MoolPath, Model],
  moolModel: mool.Model,
  moolRoot: Path
) {

  val copies: Map[Path, Path] = {
    val i = for {
      (relCfgPath, model) <- models
      (confName, conf) <- model.configurations
      (fileLanguage, files) <- conf.files
      file <- files
    } yield {

      val relative = moolRoot.relativize(file)
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount)

      val destinationFile =
        srcPath.resolve(configurationName).
          resolve(srcLanguage).
          resolve(relativeWithoutJava)

      (file, destinationFile)
    }

    i.toMap
  }
}

object Models {
  def ofMoolRepository(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty).resolveConflicts
    val models = Model.ofMoolRelCfgs(moolModel)
    Models(
      models = models,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }

  def ofMoolRepository2(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty)
    val topDown = mool.Dependency.runTopDown(moolModel)

    Models(
      models = topDown.map(d => d.rootPath -> Model.ofDescendState(d)).toMap,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }
}
