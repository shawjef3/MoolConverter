package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file._

object Main extends App {

  val moolModel = mool.Model.ofRepository(Paths.get("/Users/jshaw/git/data/vostok"))
  val models = Model.ofMoolRelCfgs(moolModel)

  for (model <- models) {
    println(model.artifactId + ": " + model.files)
  }

}
