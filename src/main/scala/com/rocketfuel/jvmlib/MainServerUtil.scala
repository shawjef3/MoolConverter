package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file.Paths

object MainServerUtil extends App {

  val path = "java.com.rocketfuel.server.util".split('.').toVector :+ "server.util"

  val moolModel = mool.Model.ofRepository(Paths.get("/Users/jshaw/git/data/vostok"))
  val relCfg = moolModel.relCfgs(path)

  val model = Model.ofMoolRelCfg(moolModel)(path, relCfg)

  println(model)

}
