package com.rocketfuel.build.jvmlib

import java.nio.file._

object MainCopyModels extends App {

  val dry = true

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Files.createTempDirectory(Paths.get("/tmp"), "mool-conversion")

  val models = Models.ofMoolRepository(moolRoot)

  val copies = models.copies(destinationRoot)

  for {
    (src, dest) <- copies
  } {
    val destPath = destinationRoot.resolve(dest)
    if (!dry) {
      Files.createDirectories(destPath.getParent)
      Files.copy(src, destPath)
    }
    println(s"cp $src $destPath")
  }

  println(copies.size)

}
