package com.rocketfuel.build.jvmlib

import java.nio.file.{Files, Paths}

object MainCreateProjectFiles extends App {

  val dry = false

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Files.createTempDirectory(Paths.get("/tmp"), "mool-conversion")

  val models = Models.ofMoolRepository(moolRoot)

  val rootProjectFiles =
    Map(
      destinationRoot.resolve("pom.xml") -> models.aggregatePom.toString,
      destinationRoot.resolve("build.sbt") -> models.aggregateSbt
    )

  for ((rootProjectFilePath, rootProjectFile) <- rootProjectFiles) {
    if (!dry) {
      Files.createDirectories(rootProjectFilePath.getParent)
      Files.write(rootProjectFilePath, rootProjectFile.getBytes)
    }
  }

  for ((_, model) <- models.models) {
    val destPath = destinationRoot.resolve(model.identifier.artifactId)
    val pom = destPath.resolve("pom.xml")
    val sbt = destPath.resolve("build.sbt")
    if (!dry) {
      Files.createDirectories(pom.getParent)
      Files.createDirectories(sbt.getParent)
      Files.write(pom, model.pom.toString.getBytes)
      Files.write(sbt, model.sbt.getBytes)
    }
    println(pom)
    println(sbt)
  }

  println(models.models.size * 2 + 2)

}
