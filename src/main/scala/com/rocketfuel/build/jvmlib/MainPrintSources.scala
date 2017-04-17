package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file._

object MainPrintSources extends App {

  val moolModel = mool.Model.ofRepository(Paths.get(System.getProperty("user.home")).resolve("git/data/vostok"))

  for {
    (modelPath, model) <- Model.ofMoolBlds(moolModel)
  } {
    val modelFiles = for {
      (configurationName, configuration) <- model.configurations
      files <- configuration.files
    } yield {
      configurationName -> files
    }
    println(modelPath -> modelFiles)
  }

}
