package com.rocketfuel.build.jvmlib

import java.nio.file._

object MainCopyModels extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Files.createTempDirectory(Paths.get("/tmp"), "mool-conversion")

  val models = Models.ofMoolRepository(moolRoot)

  models.copies(destinationRoot).foreach(println)

}
