package com.rocketfuel.mool

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.mutable

case class Model(
  root: Path,
  blds: Map[Vector[String], Map[String, Bld]],
  relCfgs: Map[Vector[String], Map[String, RelCfg]],
  versions: Map[Vector[String], Set[Version]]
) {

}

object Model {

  def ofRepository(repo: Path): Model = {
    val bldFiles = findFiles(repo, "BLD")
    val relCfgFiles = findFiles(repo, "RELCFG")
    val versionFiles = findFiles(repo, "RELCFG.versions")

    val blds =
      for (bldFile <- bldFiles) yield {
        val blds = Bld.of(repo.resolve(bldFile))
        val bldPathParts = bldFile.split("/").dropRight(1).toVector
        bldPathParts -> blds
      }

    val relCfgs =
      for (relCfgFile <- relCfgFiles) yield {
        val relCfgs = RelCfg.of(repo.resolve(relCfgFile))
        val relCfgPathParts = relCfgFile.split("/").dropRight(1).toVector
        relCfgPathParts -> relCfgs
      }

    val versions =
      for (versionFile <- versionFiles) yield {
        val versionsInFile = Version.ofFile(repo.resolve(versionFile))
        val versionFilePathParts = versionFile.split("/").dropRight(1).toVector
        versionFilePathParts -> versionsInFile
      }

    Model(
      root = repo,
      blds = blds.toMap,
      relCfgs = relCfgs.toMap,
      versions = versions.toMap
    )
  }

  def findFiles(root: Path, name: String): Vector[String] = {
    val files =
      mutable.Buffer.empty[String]

    val visitor =
      new SimpleFileVisitor[Path] {
        override def visitFile(
          file: Path,
          attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (file.getFileName.toString == name) {
            val relativePath = root.relativize(file).toString
            files.append(relativePath)
          }
          FileVisitResult.CONTINUE
        }
      }

    Files.walkFileTree(root, visitor)
    files.toVector
  }

}
