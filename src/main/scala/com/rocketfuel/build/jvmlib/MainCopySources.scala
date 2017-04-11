package com.rocketfuel.build.jvmlib

import java.nio.file._

object MainCopySources extends App {

  val byBld = args.headOption.contains("BLD")

  val dry = false

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

//  val destinationRoot = Files.createTempDirectory(Paths.get("/tmp"), "mool-conversion")
  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")
  val models =
    if (byBld)
      Models.ofMoolRepositoryByBLD(moolRoot)
    else Models.ofMoolRepositoryByRelCfg(moolRoot)

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
