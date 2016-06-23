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
      (bldPath, model) <- models
      bld = moolModel.blds(bldPath)
      srcPath = Paths.get(bldPath.drop(1).mkString("/")).resolve("src")
      (configurationName, configuration) <- model.configurations
      file <- configuration.files
    } yield {

      val relative = moolRoot.relativize(file)
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount)

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
    val moolModel = mool.Model.ofRepository(moolRoot)
    val models = Model.ofMoolBlds(moolModel)
    Models(
      models = models,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }
}
