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

  /**
    * Given a path to a relcfg, get all the paths to bld files the relcfg references.
    */
  val relCfgsToBlds: Map[(Vector[String], String), Vector[(Vector[String], String)]] = {
    for {
      (path, pathRelCfgs) <- relCfgs
      (name, relCfg) <- pathRelCfgs
    } yield {
      ((path, name), relCfg.`jar-with-dependencies`.toVector.flatMap(artifact => Vector((artifact.targetPath, artifact.targetName))))
    }
  }

  /**
    * Given a bld, get all the paths to blds that it depends on. Intransitive.
    */
  val bldsToBlds: Map[(Vector[String], String), Vector[(Vector[String], String)]] = {
    for {
      (bldPath, pathBlds) <- blds
      (bldName, bld) <- pathBlds
    } yield {
      val deps =
        for {
          dep <- bld.deps.getOrElse(Vector.empty)
        } yield {
          val depParts = dep.split('.').toVector
          val depPath = depParts.drop(1).dropRight(1)
          (depPath, depParts.last)
        }
      (bldPath, bldName) -> deps
    }
  }

  /**
    * Given a path to a bld, get all the paths to blds that it depends on. Transitive.
    *
    * The values are streams, because they will be infinite when there is a
    * circular dependency.
    */
  val bldToBldsTransitive: Map[(Vector[String], String), Stream[(Vector[String], String)]] = {
    def aux(bldPath: Vector[String], bldName: String): Stream[(Vector[String], String)] = {
      val bld = blds(bldPath)(bldName)
      val immediateDependencies = bld.deps.getOrElse(Vector.empty).toStream.map{dependency =>
        val split = dependency.split('.').toVector
        (split.drop(1).dropRight(1), split.last)
      }
      immediateDependencies ++ immediateDependencies.flatMap(pathAndName => bldToBldsTransitive(pathAndName._1, pathAndName._2))
    }

    for {
      (bldPath, pathBlds) <- blds
      (bldName, _) <- pathBlds
    } yield (bldPath, bldName) -> aux(bldPath, bldName)
  }

  /**
    * Given a path to a RelCfg, get all the paths to blds that it depends on. Transitive.
    *
    * The values are streams, because they will be infinite when there is a
    * circular dependency.
    */
  val relCfgsToBldsTransitive: Map[(Vector[String], String), Stream[(Vector[String], String)] = {
    def aux(relCfgPath: Vector[String], relCfgName: String): Stream[(Vector[String], String)] = {
      val relCfg = relCfgs(relCfgPath)(relCfgName)
      for {
        bld <- relCfg.`jar-with-dependencies`.toStream
        transitiveDependency <- (bld.targetPath, bld.targetName) #:: bldToBldsTransitive(bld.targetPath, bld.targetName)
      } yield transitiveDependency
    }

    for {
      (relCfgPath, pathRelCfgs) <- relCfgs
      (relCfgName, _) <- pathRelCfgs
    } yield (relCfgPath, relCfgName) -> aux(relCfgPath, relCfgName)
  }

  /**
    * Given a path to a bld, get all the paths to relcfgs that directly reference the bld.
    */
  val bldsToRelCfgs: Map[(Vector[String], String), Vector[(Vector[String], String)]] = {
    for {
      (bldPath, pathBlds) <- blds
      (bldName, _) <- pathBlds
    } yield {
      val bldRelCfgs =
        for {
          (relCfgPath, pathRelCfgs) <- relCfgs.toVector
          (relCfgName, relCfg) <- pathRelCfgs.toVector
          if relCfg.`jar-with-dependencies`.exists(target => target.targetPath == bldPath && target.targetName == bldName)
        } yield (relCfgPath, relCfgName)
      (bldPath, bldName) -> bldRelCfgs
    }
  }

  val bldsToRelCfgsTransitive: Map[(Vector[String], String), Iterable[(Vector[String], String)]] = {
    val one =
      for {
        (relCfg, blds) <- relCfgsToBldsTransitive
        bld <- blds
      } yield (bld, relCfg)
    val grouped = one.groupBy(_._1)
    grouped.map {
      case (key, value) =>
        (key, value.values)
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
