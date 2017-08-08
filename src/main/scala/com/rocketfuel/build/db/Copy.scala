package com.rocketfuel.build.db

import java.nio.file.{Files, Path, StandardCopyOption, StandardOpenOption}

case class Copy(
  source: String,
  packagePath: String,
  destination: String
)

object Copy {

  /**
    * Run multiple copies.
    *
    * Only copies a file once. If multiple copies of the same file are
    * required, then links are created to the first copy.
    */
  def copyFiles(
    copies: Set[Copy],
    sourceRoot: Path,
    destinationRoot: Path
  ): Map[String, Path] = {
    val protoCopies =
      copies.filter(_.source.endsWith(".proto"))

    copies.foldLeft(Map.empty[String, Path]) {
      case (previousCopies, Copy(source, _, destination)) =>
        val destinationPath = destinationRoot.resolve(destination)
        Files.createDirectories(destinationPath.getParent)

        previousCopies.get(source) match {
          case None =>
            val sourcePath = sourceRoot.resolve(source)
            copyFile(protoCopies, sourcePath, destinationPath)
            previousCopies + (source -> destinationPath)

          case Some(existingDestination) =>
            val target = destinationPath.getParent.relativize(existingDestination)
            Files.deleteIfExists(destinationPath)
            Files.createSymbolicLink(destinationPath, target)
            previousCopies
        }
    }
  }

  def copyFile(protoCopies: Set[Copy], source: Path, destination: Path): Unit = {
    if (source.getFileName.toString.endsWith(".proto")) {
      copyProto(protoCopies, source, destination)
    } else {
      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  /**
    * Most files can be copied verbatim. However, protoc files have imports that use the literal path
    * to dependent protoc files. These paths change with the conversion to maven projects.
    */
  def copyProto(protoCopies: Set[Copy], source: Path, destination: Path): Unit = {
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

    Files.write(destination, destinationContents.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
  }
}
