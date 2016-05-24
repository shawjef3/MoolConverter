package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file._

object MainPrintSources extends App {

  val moolModel = mool.Model.ofRepository(Paths.get(System.getProperty("user.home")).resolve("git/data/vostok"))

  for {
    (modelPath, model) <- Model.ofMoolBlds(moolModel)
  } {
    val modelFiles = for {
      (configurationName, configuration) <- model.configurations
    } yield {
      val dependencyFiles =
        configuration.dependencies.flatMap {
          case Model.Dependency.Local(depPath) =>
            val dep = moolModel.blds(depPath)
            dep.srcPaths(moolModel, depPath)
          case x: Model.Dependency.Remote =>
            Vector[Path]()
        }

      configurationName -> (configuration.files.toSet ++ dependencyFiles)
    }
    println(modelPath -> modelFiles)
  }

}
