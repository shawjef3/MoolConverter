package com.rocketfuel.build.mool

import com.rocketfuel.build.GroupByKeys._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.mutable

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
    * blds to blds that depend on them
    */
  val bldsToBldsReverse: Map[MoolPath, Set[MoolPath]] = {
    val all = for {
      (src, dsts) <- bldsToBlds.toTraversable
      dst <- dsts
    } yield dst -> src

    all.groupByKeys
  }

  val bldsToCompileBlds: Map[MoolPath, Set[MoolPath]] = {
    for {
      (bldPath, bld) <- blds
    } yield {
      bldPath -> bld.compileDepPaths(bldPath).toSet
    }
  }

  /**
    * blds to blds that depend on them for compiling
    */
  val bldsToCompileBldsReverse: Map[MoolPath, Set[MoolPath]] = {
    val all = for {
      (src, dsts) <- bldsToCompileBlds.toTraversable
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

  /**
    * Given a path to a Bld, get all the paths to Blds that it depends on for compilation. Transitive.
    */
  val bldsToCompileBldsTransitive: Map[MoolPath, Set[MoolPath]] = {
    transitive(bldsToCompileBlds)
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
    /*
    Bld to test bld, where the name of the bld to be tested is the same
    as the test Bld, except it doesn't end in 'Test'.
     */
    val withoutTest =
      for {
        (testBldPath, testBld) <- testBlds.toTraversable
        if testBldPath.nonEmpty && testBldPath.last.endsWith("Test")
        bldPath = testBldPath.init :+ testBldPath.last.dropRight(4)
        if blds.contains(bldPath) && testBld.depPaths(testBldPath).contains(bldPath)
      } yield bldPath -> testBldPath

    /*
    Bld to test bld, when there is only non-test, non-maven dep.
     */
    val onlyOneDep =
      for {
        (testBldPath, testBld) <- testBlds.toTraversable
        depPaths = bldsToBlds(testBldPath)
        deps = blds.filterKeys(depPaths.contains)
        regularDeps = deps.filterNot(bld => bld._2.rule_type.contains("test") || bld._2.maven_specs.nonEmpty)
        if regularDeps.size == 1
      } yield regularDeps.head._1 -> testBldPath

      withoutTest.groupByKeys ++
        onlyOneDep.groupByKeys ++
        bldToTestBldSupplement
  } withDefaultValue Set.empty

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
      (relCfgPath, blds) <- relCfgsToBldsTransitive
    } yield {
      relCfgPath ->
        blds.filter { bldPath =>
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
        (relCfg, blds) <- relCfgsToBldsTransitive.toTraversable //allow duplicates
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
        (testBldPath, testBld) <- testBlds.toIterable
        bldDepPath <- testBld.depPaths(testBldPath)
        relCfgPath <- bldsToRelCfgsTransitive(bldDepPath)
      } yield relCfgPath -> testBldPath

    toReduce.groupByKeys.withDefaultValue(Set.empty)
  }

  val testBldsToRelCfgs: Map[MoolPath, Set[MoolPath]] = {
    val toReduce =
      for {
        (testBldPath, testBld) <- testBlds.toIterable
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
    val indirectBldToRelCfg =
      for {
        (relCfgPath, bldPaths) <- relCfgsToBldsTransitive.toTraversable
        bldPath <- bldPaths
      } yield bldPath -> relCfgPath

    for {
      (bldPath, relCfgPaths) <- indirectBldToRelCfg.groupByKeys
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
            base_version = "0.0",
            `jar-with-dependencies` = Some(RelCfg.Artifact(target = ("mool" +: bldPath).mkString("."), artifact_path = (bldPath.init :+ bldPath.last + ".jar").mkString("/"))),
            `jar-no-dependencies` = None
          )
        bldPath -> relCfg
      }

    copy(
      relCfgs = relCfgs ++ newRelCfgs
    )
  }

}

object Model {

  val javaRuleTypes =
    Set("file_coll", "java_lib", "java_proto_lib", "java_test", "release_package", "scala_lib", "scala_test")

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
      versions = versions.toMap,
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
