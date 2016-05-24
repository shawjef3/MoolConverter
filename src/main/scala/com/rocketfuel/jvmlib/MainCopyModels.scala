package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file._

object MainCopyModels extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val moolModel = mool.Model.ofRepository(moolRoot)

  val destinationRoot = Files.createTempDirectory(Paths.get("/tmp"), "mool-conversion")

  val models = Models.ofMoolRepository(moolRoot)

  models.copies(destinationRoot).foreach(println)

}
