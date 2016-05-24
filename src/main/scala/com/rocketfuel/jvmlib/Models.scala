package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file.Path

case class Models(
  models: Map[mool.MoolPath, Model],
  moolModel: mool.Model,
  moolRoot: Path
) {

  def copies(destinationSrcRoot: Path): Map[Path, Path] = {
    for {
      (modelPath, model) <- models
      (configurationName, configuration) <- model.configurations
      dependency <- configuration.dependencies
      toCopy <- dependency match {
        case Model.Dependency.Local(depPath) =>
          val depBld = moolModel.blds(depPath)
          for {
            toCopy <- depBld.srcPaths(moolModel, depPath)
          } yield {
            val relative = moolRoot.relativize(toCopy)
            val relativeWithoutJava = relative.subpath(1, relative.getNameCount - 1)
            val srcLanguage = "\\.([^.]+)$".r.findFirstMatchIn(relative.toString).get.group(1)
            val destinationFile = destinationSrcRoot.resolve(depPath.last).resolve("src").resolve(configurationName).resolve(srcLanguage).resolve(relativeWithoutJava)
            (toCopy, destinationFile)
          }
        case x: Model.Dependency.Remote =>
          Vector[(Path, Path)]()
      }
    } yield toCopy
  }

}

object Models {
  def ofMoolRepository(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot)
    Models(
      models = Model.ofMoolRelCfgs(moolModel),
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }
}
