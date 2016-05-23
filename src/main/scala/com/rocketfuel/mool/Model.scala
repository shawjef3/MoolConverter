package com.rocketfuel.mool

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.mutable

case class Model(
  root: Path,
  blds: Map[MoolPath, Bld],
  relCfgs: Map[MoolPath, RelCfg],
  versions: Map[MoolPath, Set[Version]]
) {

  /**
    * Given a path to a bld, get all the paths to blds that it depends on. Intransitive.
    */
  val bldsToBlds: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bldPath, bld) <- blds
    } yield {
      bldPath -> bld.depPaths.toSet
    }
  }

  /**
    * Given a path to a bld, get all the paths to blds that it depends on. Transitive.
    */
  val bldToBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    def aux(accumulator: Set[MoolPath], bldStack : Vector[MoolPath]): Set[MoolPath] = {
      bldStack.headOption match {
        case None =>
          accumulator

        case Some(depPath) =>
          val depBld = blds(depPath)
          val depPaths = depBld.deps.getOrElse(Vector.empty).map(Bld.path)
          val newDepPaths = depPaths.filter(! accumulator.contains(_))

          val newAccumulator = accumulator ++ newDepPaths
          val newStack = bldStack.tail ++ newDepPaths
          aux(newAccumulator, newStack)
      }
    }

    for {
      (bldPath, _) <- blds
    } yield bldPath -> aux(Set.empty, Vector(bldPath))
  }

  /**
    * Given a path to a bld, get all the paths to relcfgs that directly reference the bld.
    */
  val bldsToRelCfgs: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bldPath, _) <- blds
    } yield {
      val bldRelCfgs =
        for {
          (relCfgPath, relCfg) <- relCfgs
          if relCfg.`jar-with-dependencies`.exists(target => target.targetPath == bldPath)
        } yield relCfgPath
      bldPath -> bldRelCfgs.toSet
    }
  }

  /**
    * Given a path to a relcfg, get the path to bld file the relcfg references.
    */
  val relCfgsToBld: Map[MoolPath, Option[MoolPath]] = {
    for {
      (path, relCfg) <- relCfgs
    } yield {
      (path, relCfg.`jar-with-dependencies`.map(_.targetPath))
    }
  }

  /**
    * Given a path to a RelCfg, get all the paths to blds that it depends on. Transitive.
    *
    * The values are streams, because they will be infinite when there is a
    * circular dependency.
    */
  val relCfgsToBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    for {
      (relCfgPath, _) <- relCfgs
    } yield relCfgPath -> {
      val relCfg = relCfgs(relCfgPath)
      for {
        bld <- relCfg.`jar-with-dependencies`.toSet[RelCfg.Artifact]
        transitiveDependency <- bldToBldsTransitive(bld.targetPath) + bld.targetPath
      } yield transitiveDependency
    }
  }

  /**
    * Given a path to a bld, get all the relcfgs that depend on it, following
    * transitive bld dependencies.
    */
  val bldsToRelCfgsTransitive: Map[MoolPath, Set[MoolPath]] = {
    val one =
      for {
        (relCfg, blds) <- relCfgsToBldsTransitive
        bld <- blds
      } yield (bld, relCfg)
    val grouped = one.groupBy(_._1)
    grouped.map {
      case (key, value) =>
        (key, value.map(_._2).toSet)
    }
  }

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
        for {
          (bldName, bld) <- blds
        } yield (bldPathParts :+ bldName) -> bld
      }

    val relCfgs =
      for (relCfgFile <- relCfgFiles) yield {
        val relCfgs = RelCfg.of(repo.resolve(relCfgFile))
        val relCfgPathParts = relCfgFile.split("/").dropRight(1).toVector
        for {
          (relCfgName, relCfg) <- relCfgs
        } yield (relCfgPathParts :+ relCfgName) -> relCfg
      }

    val versions =
      for (versionFile <- versionFiles) yield {
        val versionsInFile = Version.ofFile(repo.resolve(versionFile))
        val versionFilePathParts = versionFile.split("/").dropRight(1).toVector
        versionFilePathParts -> versionsInFile
      }

    Model(
      root = repo,
      blds = blds.foldLeft(Map.empty[MoolPath, Bld])(_ ++ _),
      relCfgs = relCfgs.foldLeft(Map.empty[MoolPath, RelCfg])(_ ++ _),
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
