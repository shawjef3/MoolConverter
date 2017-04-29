package com.rocketfuel.build.db.mvn

import java.nio.file.{Files, Path, StandardCopyOption}

/**
  * Most files can be copied verbatim. However, protoc files have imports that use the literal path
  * to dependent protoc files. These paths change with the conversion to maven projects.
  */
case class FileCopier(
  copies: Set[Copy],
  sourceRoot: Path,
  destinationRoot: Path
) {

  val protoCopies =
    copies.filter(_.source.endsWith(".proto"))

  def copyAll(): Unit = {
    for (Copy(source, _, destination) <- copies)
      copy(source, destination)
  }

  def copy(source: String, destination: String): Unit = {
    val sourcePath = sourceRoot.resolve(source)
    val destinationPath = destinationRoot.resolve(destination)

    Files.createDirectories(destinationPath.getParent)
    if (source.endsWith(".proto"))
      copyProto(sourcePath, destinationPath)
    else Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
  }

  protected def copyProto(source: Path, destination: Path): Unit = {
    val sourceContents = new String(Files.readAllBytes(source))

    val destinationContents =
      protoCopies.foldLeft(sourceContents) {
        case (accum, Copy(importSource, importPackagePath, _)) =>
          /*
          The protoc maven plugin puts the protoc files in the proto_path for us,
          so we just have to give the path relative to the source root.
           */
          accum.replace(importSource, importPackagePath)
      }

    Files.write(destination, destinationContents.getBytes)
  }

}
