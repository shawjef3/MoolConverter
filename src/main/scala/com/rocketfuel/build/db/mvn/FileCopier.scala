package com.rocketfuel.build.db.mvn

import java.nio.charset.MalformedInputException
import java.nio.file.{Files, Path, StandardCopyOption, StandardOpenOption}

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
    if (source.endsWith(".proto")) {
      copyProto(sourcePath, destinationPath)
    } else {
      FileCopier.copyFixed(sourcePath, destinationPath)
    }
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

    Files.write(destination, destinationContents.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
  }

}

object FileCopier {

  val replacements = Map(
    "\"/java/com/rocketfuel/modeling/athena/testdata/".r -> "\"/testdata/",
    "\"\\./java/com/rocketfuel/modeling/athena/testdata".r -> "./testdata",
    "System\\.getenv\\(\"BUILD_ROOT\"\\)".r -> "System.getProperty(\"user.dir\")"
  )

  def fixAthenaTestData(s: String): String = {
    replacements.foldRight(s) {
      case ((matcher, replacement), accum) =>
        matcher.replaceAllIn(accum, replacement)
    }
  }

  def copyAthenaTestFile(source: Path, destination: Path): Unit = {
    Files.createDirectories(destination.getParent)
    try {
      val sourceContents = io.Source.fromFile(source.toFile).mkString
      val destinationContents = FileCopier.fixAthenaTestData(sourceContents)
      Files.write(destination,
        destinationContents.getBytes("UTF-8"),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
    } catch {
      case _: MalformedInputException =>
        //it's not a text file
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  def copyAthenaTestFiles(source: Path, destination: Path): Unit = {
    Files.createDirectories(destination)
    Files.walk(source).filter(Files.isRegularFile(_)).forEach(
      path => {
        val relativePath = source.relativize(path)
        val pathDestination = destination.resolve(relativePath)
        copyAthenaTestFile(path, pathDestination)
      }
    )
  }

  def copyFixed(source: Path, destination: Path): Unit = {
    try {
      val sourceContents = io.Source.fromFile(source.toFile).mkString
      val destinationContents = fixAthenaTestData(sourceContents)
      Files.createDirectories(destination.getParent)
      Files.write(destination, destinationContents.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    } catch {
      case _: MalformedInputException =>
        //it's not a text file
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
    }
  }

}
