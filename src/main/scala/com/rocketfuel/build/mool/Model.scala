package com.rocketfuel.build.mool

import com.rocketfuel.build.GroupByKeys._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.mutable
import scalaz.StrictTree

/**
  *
  * @param root
  * @param blds
  * @param relCfgs
  * @param versions
  * @param bldToTestBldSupplement contains cases to override the test bld heuristic.
  */
case class Model(
  root: Path,
  blds: Map[MoolPath, Bld],
  relCfgs: Map[MoolPath, RelCfg],
  versions: Map[MoolPath, Set[Version]],
  bldToTestBldSupplement: Map[MoolPath, Set[MoolPath]]
) extends Graphviz {

  val maxVersion: Map[MoolPath, String] =
    for {
      (bldPath, bldVersions) <- versions
    } yield bldPath -> bldVersions.max.version.mkString(".")

  /**
    * Given a path to a Bld, get all the paths to Blds that it depends on. Intransitive.
    */
  val bldsToBlds: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bldPath, bld) <- blds
    } yield {
      bldPath -> (bld.depPaths(bldPath).toSet ++ bld.compileDepPaths(bldPath).toSet)
    }
  }

  /**
    * blds to blds that depend on them
    */
  val bldsToBldsReverse: Map[MoolPath, Set[MoolPath]] = {
    val all = for {
      (src, dsts) <- bldsToBlds.toSeq
      dst <- dsts
    } yield dst -> src

    all.groupByKeys
  }

  private def transitive(bldsToDeps: Map[MoolPath, Set[MoolPath]]): Map[MoolPath, Set[MoolPath]] = {
    def aux(accumulator: Set[MoolPath], bldStack: Vector[MoolPath]): Set[MoolPath] = {
      bldStack.headOption match {
        case None =>
          accumulator

        case Some(bldPath) =>
          val deps = bldsToDeps(bldPath)
          val newDepPaths = deps -- accumulator

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
    * Given a path to a Bld, get all the paths to Blds that it depends on. Transitive.
    */
  val bldsToBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    transitive(bldsToBlds)
  }

  val testBlds: Map[MoolPath, Bld] = {
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
  val bldsToTestBlds: Map[MoolPath, Set[MoolPath]] = {
    for {
      (testPath, testBld) <- testBlds.toSeq
      dependency <- testBld.depPaths(testPath)
    } yield dependency -> testPath
  }.groupByKeys.withDefaultValue(Set.empty)

  /**
    * Test blds that aren't attributed to any bld.
    */
  val bldsToTestBldsRemainders: Set[MoolPath] = {
    testBlds.keySet -- bldsToTestBlds.values.flatten
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
    */
  val relCfgsToBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    for {
      (relCfgPath, relCfg) <- relCfgs
    } yield relCfgPath -> {
      for {
        bld <- relCfg.`jar-with-dependencies`.toSet[RelCfg.Artifact]
        transitiveDependency <- bldsToBldsTransitive(bld.targetPath) + bld.targetPath
      } yield transitiveDependency
    }
  }

  /**
    * Given a path to a RelCfg, get paths to Blds that it depends on
    * that are not also depended on by another RelCfg. Transitive.
    */
  val relCfgsToExclusiveBlds: Map[MoolPath, Set[MoolPath]] = {
    for {
      (relCfgPath, bldPaths) <- relCfgsToBldsTransitive
    } yield {
      relCfgPath ->
        bldPaths.filter { bldPath =>
          ! relCfgsToBldsTransitive.exists {
            case (otherRelCfgPath, otherBlds) =>
              otherRelCfgPath != relCfgPath &&
                otherBlds.contains(bldPath)
          }
        }
    }
  }

  /**
    * Given a path to a Bld, get all the RelCfgs that depend on it, following
    * transitive Bld dependencies.
    */
  val bldsToRelCfgsTransitive: Map[MoolPath, Set[MoolPath]] = {
    val one =
      for {
        (relCfg, blds) <- relCfgsToBldsTransitive.toSeq //allow duplicates
        bld <- blds
      } yield (bld, relCfg)

    one.groupByKeys.withDefaultValue(Set.empty)
  }

  /**
    * Get all the test Blds that belong to a RelCfg.
    */
  val relCfgToTestBlds: Map[MoolPath, Set[MoolPath]] = {
    val toReduce =
      for {
        (testBldPath, testBld) <- testBlds.toSeq
        bldDepPath <- testBld.depPaths(testBldPath)
        relCfgPath <- bldsToRelCfgsTransitive(bldDepPath)
      } yield relCfgPath -> testBldPath

    toReduce.groupByKeys.withDefaultValue(Set.empty)
  }

  val testBldsToRelCfgs: Map[MoolPath, Set[MoolPath]] = {
    val toReduce =
      for {
        (testBldPath, testBld) <- testBlds.toSeq
        depPath <- testBld.depPaths(testBldPath)
        relCfgPath <- bldsToRelCfgsTransitive(depPath)
      } yield testBldPath -> relCfgPath

    toReduce.groupByKeys.withDefaultValue(Set.empty)
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
  val bldConflicts: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bldPath, relCfgPaths) <- bldsToRelCfgsTransitive
      //blds that are directly referenced by a relcfg are not conflicted
      if !relCfgPaths.exists(relCfgPath => relCfgsToBld(relCfgPath).contains(bldPath))
      //blds that are referenced by only one relcfg are not conflicted
      if relCfgPaths.size > 1
      //blds that are external to the project are not conflicted
      if blds(bldPath).maven_specs.isEmpty
    } yield bldPath -> relCfgPaths
  }

  /**
    * These Blds are not referenced by any RelCfg, transitively or directly.
    */
  val bldOrphans = {
    blds.keySet -- relCfgsToBldsTransitive.flatMap(_._2)
  }

  /**
    * These test Blds are not referenced by any RelCfg, transitively or directly.
    */
  val testBldOrphans: Set[MoolPath] = {
    for {
      (bldPath, bld) <- testBlds
      //If all the Blds that the test Bld depends on are orphaned, then the test Bld is orphaned too.
      if bldsToBlds(bldPath).forall(bldOrphans.contains)
    } yield bldPath
  } toSet

  /**
    * These test Blds are referenced by more than one RelCfg.
    */
  val testBldConflicts: Set[MoolPath] = {
    testBldsToRelCfgs.filter(_._2.size > 1).keySet
  }

  /**
    * Create a new model where every conflicted Bld has its own RelCfg
    */
  lazy val resolveConflicts: Model = {
    val newRelCfgs =
      for {
        (bldPath, relCfgPaths) <- bldConflicts
      } yield {
        val relCfg =
          RelCfg(
            group_id = bldPath.init.mkString("."),
            artifact_id = bldPath.last,
            base_version = "0",
            `jar-with-dependencies` = Some(RelCfg.Artifact(target = ("mool" +: bldPath).mkString("."), artifact_path = (bldPath.init :+ bldPath.last + ".jar").mkString("/"))),
            `jar-no-dependencies` = None
          )
        bldPath -> relCfg
      }

    copy(
      relCfgs = relCfgs ++ newRelCfgs
    )
  }

  lazy val bldTree: Vector[StrictTree[MoolPath]] =
    StrictTree.unfoldForest(blds.toVector.map(bld => (bld, Set.empty[MoolPath]))) {
      case ((moolPath, bld), history) if history.contains(moolPath) =>
        (moolPath, Vector.empty)
      case ((moolPath, bld), history) =>
        (moolPath, bldsToBlds(moolPath).toVector.map(path => ((path, blds(path)), history + path)))
    }

}

object Model {

  val javaRuleTypes =
    Set("file_coll", "java_lib", "java_bin", "java_proto_lib", "java_test", "release_package", "scala_lib", "scala_test")

  def ofRepository(
    repo: Path,
    bldToTestBldSupplement: Map[MoolPath, Set[MoolPath]]
  ): Model = {
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

    val versions = {
      for {
        versionFile <- versionFiles
        versionsInFile = Version.ofFile(repo.resolve(versionFile))
        versionFilePathParts = versionFile.split("/").dropRight(1).toVector
        (groupId, groupVersions) <- versionsInFile.groupBy(_.groupId)
      } yield (versionFilePathParts :+ groupId) -> groupVersions
    }.toMap

    Model(
      root = repo,
      blds = blds.foldLeft(Map.empty[MoolPath, Bld])(_ ++ _),
      relCfgs = relCfgs.foldLeft(Map.empty[MoolPath, RelCfg])(_ ++ _),
      versions = versions,
      bldToTestBldSupplement
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
