package com.rocketfuel.build.mool

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
    * Given a path to a Bld, get all the paths to Blds that it depends on. Intransitive.
    */
  val bldsToBlds: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bldPath, bld) <- blds
    } yield {
      bldPath -> bld.depPaths(bldPath).toSet
    }
  }

  /**
    * Given a path to a Bld, get all the paths to Blds that it depends on. Transitive.
    */
  val bldsToBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    def aux(accumulator: Set[MoolPath], bldStack : Vector[MoolPath]): Set[MoolPath] = {
      bldStack.headOption match {
        case None =>
          accumulator

        case Some(bldPath) =>
          val bld = blds(bldPath)
          val depPaths = bld.depPaths(bldPath)
          val newDepPaths = depPaths.toSet -- accumulator

          val newAccumulator = accumulator ++ newDepPaths
          val newStack = bldStack.tail ++ newDepPaths
          aux(newAccumulator, newStack)
      }
    }

    for {
      (bldPath, _) <- blds
    } yield bldPath -> aux(Set.empty, Vector(bldPath))
  }

  val testBlds = {
    blds.filter(_._2.rule_type.contains("test"))
  }

  /**
    * Given a path to a Bld, get all the paths to testing Blds that depend on it.
    * Intransitive.
    *
    * This is used to pull test classes into the same project as a Bld that is targeted
    * by a RelCfg, because RelCfgs never reference a testing Bld.
    *
    * Mool is odd in that the testing Blds depend on the Bld being tested, rather than
    * the test files being part of the same Bld.
    */
  val bldToTestBlds: Map[MoolPath, Set[MoolPath]] = {
    val dependencyToTestBld =
      for {
        (testBldPath, bld) <- testBlds.toTraversable //allow duplicate keys
        dependencies = bldsToBlds(testBldPath)
        dependency <- dependencies
      } yield dependency -> testBldPath

    for {
      (dependency, dependencyAndTestBldPaths) <- dependencyToTestBld.groupBy(_._1)
    } yield dependency -> dependencyAndTestBldPaths.map(_._2).toSet
  }

  val bldToTestBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bld, testBlds) <- bldToTestBlds
    } yield {
      val testBldsDependencies =
        for {
          testBld <- testBlds
          testBldDependency <- bldsToBldsTransitive(testBld) + testBld
        } yield testBldDependency
      bld -> testBldsDependencies
    }
  }

  /**
    * Given a path to a Bld, get all the paths to RelCfgs that directly reference the Bld.
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
    * Given a path to a RelCfg, get the path to Bld file the RelCfg references.
    */
  val relCfgsToBld: Map[MoolPath, Option[MoolPath]] = {
    for {
      (path, relCfg) <- relCfgs
    } yield {
      (path, relCfg.`jar-with-dependencies`.map(_.targetPath))
    }
  }

  /**
    * Given a path to a RelCfg, get all the paths to Blds that it depends on. Transitive.
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
        transitiveDependency <- bldsToBldsTransitive(bld.targetPath) + bld.targetPath
      } yield transitiveDependency
    }
  }

  /**
    * Given a path to a Bld, get all the RelCfgs that depend on it, following
    * transitive Bld dependencies.
    */
  val bldsToRelCfgsTransitive: Map[MoolPath, Set[MoolPath]] = {
    //For Blds that have no RelCfg
    val defaults =
      blds.map(_.copy(_2 = Set.empty[MoolPath]))

    val one =
      for {
        (relCfg, blds) <- relCfgsToBldsTransitive.toTraversable //allow duplicates
        bld <- blds
      } yield (bld, relCfg)

    val grouped = one.groupBy(_._1)

    val groupedWithoutKey = grouped.map {
      case (key, value) =>
        (key, value.map(_._2).toSet)
    }

    defaults ++ groupedWithoutKey
  }

  /**
    * A map of Blds that are directly referenced by at least two RelCfgs.
    */
  val bldDirectConflicts = {
    bldsToRelCfgs.filter(_._2.size > 1).keySet
  }

  /**
    * A map of Blds that are indirectly referenced by at least two RelCfgs, but
    * directly referenced by no RelCfgs, to the RelCfgs that indirectly reference
    * them.
    *
    * These Blds and their dependencies will need to be put into
    * their own libraries, to prevent duplication.
    *
    * Blds for maven dependencies are ignored.
    */
  val bldIndirectConflicts = {
    val indirectBlds =
      relCfgsToBldsTransitive.map {
        case (relCfgPath, transitiveBlds) =>
          (relCfgPath, transitiveBlds -- relCfgsToBld.get(relCfgPath).flatten.toSet)
      }

    val indirectBldToRelCfg = for {
      (relCfgPath, bldPaths) <- indirectBlds.toTraversable
      bldPath <- bldPaths
    } yield (bldPath, relCfgPath)

    for {
      (bldPath, bldPathAndRelCfgPaths) <- indirectBldToRelCfg.groupBy(_._1)
      if bldPathAndRelCfgPaths.size > 1 && blds(bldPath).maven_specs.isEmpty
    } yield (bldPath, bldPathAndRelCfgPaths.map(_._2).toSet)
  }

  /**
    * These Blds are not referenced by any RelCfg, transitively or directly.
    *
    * This doesn't include test Blds.
    */
  val bldOrphans = {
    (blds.filter(! _._2.rule_type.contains("test")) -- relCfgsToBldsTransitive.flatMap(_._2)).keySet
  }

  /**
    * These test Blds are not referenced by any RelCfg, transitively or directly.
    */
  val testBldOrphans = {
    for {
      (bldPath, bld) <- testBlds
      //If all the Blds that the test Bld depends on are orphaned, then the test Bld is orphaned too.
      if bldsToBlds(bldPath).forall(bldOrphans.contains)
    } yield bldPath
  } toSet

}

object Model {

  val javaRuleTypes =
    Set("file_coll", "java_lib", "java_proto_lib", "java_test", "release_package", "scala_lib", "scala_test")

  def ofRepository(repo: Path): Model = {
    val bldFiles = findFiles(repo, "BLD")
    val relCfgFiles = findFiles(repo, "RELCFG")
    val versionFiles = findFiles(repo, "RELCFG.versions")

    val blds =
      for {
        bldFile <- bldFiles
      } yield {
        val blds = Bld.of(repo.resolve(bldFile))
        val bldPathParts = bldFile.split("/").dropRight(1).toVector
        for {
          (bldName, bld) <- blds
          if javaRuleTypes.contains(bld.rule_type)
        } yield (bldPathParts :+ bldName) -> bld
      }

    val relCfgs =
      for (relCfgFile <- relCfgFiles) yield {
        val relCfgs = RelCfg.of(repo.resolve(relCfgFile))
        val relCfgPathParts = relCfgFile.split("/").dropRight(1).toVector
        for {
          (relCfgName, relCfg) <- relCfgs
          //filter out non-java RelCfgs.
          if relCfg.`jar-with-dependencies`.nonEmpty
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
