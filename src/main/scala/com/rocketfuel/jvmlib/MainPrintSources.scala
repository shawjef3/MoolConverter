package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file._

object MainPrintSources extends App {

  val moolModel = mool.Model.ofRepository(Paths.get(System.getProperty("user.home")).resolve("git/data/vostok"))

  for {
    (modelPath, model) <- Model.ofMoolRelCfgs(moolModel)
  } {
    println(model.artifactId)
    val depFiles = for {
      dependency <- model.dependencies
    } yield {
      dependency match {
        case Model.Dependency.Local(depPath) =>
          val dep = moolModel.blds(depPath)
          dep.srcPaths(moolModel, depPath)
        case x: Model.Dependency.Remote =>
          Vector[Path]()
      }
    }

    val srcFiles = model.files ++ depFiles.flatten
    println(srcFiles.size)
    for (srcFile <- srcFiles) {
      println(s"$srcFile: ${Files.exists(srcFile)}")
    }

  }
}
