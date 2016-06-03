package com.rocketfuel.build.mool

import java.nio.file.Paths

class ModelApp extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val model = Model.ofRepository(moolRoot)

}
