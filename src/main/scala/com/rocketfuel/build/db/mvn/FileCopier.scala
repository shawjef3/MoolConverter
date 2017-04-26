package com.rocketfuel.build.db.mvn

import java.nio.file.{Files, Path}

/**
  * Most files can be copied verbatim. However, protoc files
  */
case class FileCopier(
  copies: Map[String, String],
  sourceRoot: Path,
  destinationRoot: Path
) {

  val protoCopies =
    copies.filter(_._1.endsWith(".proto"))

  def copyAll(): Unit = {
    for ((source, destination) <- copies)
      copy(source, destination)
  }

  def copy(source: String, destination: String): Unit = {
    val sourcePath = sourceRoot.resolve(source)
    val destinationPath = destinationRoot.resolve(destination)

    Files.createDirectories(destinationPath.getParent)
    if (source.endsWith(".proto"))
      copyProto(sourcePath, destinationPath)
    else Files.copy(sourcePath, destinationPath)
  }

  protected def copyProto(source: Path, destination: Path): Unit = {
    val sourceContents = new String(Files.readAllBytes(source))

    val destinationContents =
      protoCopies.foldLeft(sourceContents) {
        case (accum, (importSource, importDestination)) =>
          val importDestinationPath = destinationRoot.resolve(importDestination)
          val importRelativePath = destination.relativize(importDestinationPath)
          accum.replace(importSource, importRelativePath.toString)
      }

    Files.write(destination, destinationContents.getBytes)
  }

}

object FileCopier {
  def ofCopies(
    copies: Set[Copy],
    sourceRoot: Path,
    destinationRoot: Path
  ): FileCopier = {
    val fileMap =
      for (Copy(source, destination) <- copies)
        yield source -> destination

    FileCopier(fileMap.toMap, sourceRoot, destinationRoot)
  }
}
