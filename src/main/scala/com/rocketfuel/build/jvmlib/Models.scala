package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.{Logger, mool}
import com.rocketfuel.build.mool.{Configurations, _}
import java.nio.file._
import scalaz._
import Scalaz._
import scala.collection.parallel.ParIterable

case class Models(
  models: Map[mool.MoolPath, Model],
  moolModel: mool.Model,
  moolRoot: Path
) {

  def copies(srcPath: Path): Map[Path, Path] = {
    for {
      (relCfgPath, model) <- models
      projectRoot = srcPath.resolve(relCfgPath.last)
      (confName, conf) <- model.configurations
      (srcLanguage, files) <- conf.files
      file <- files
    } yield {
      val relative = moolRoot.relativize(file)
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount)

      val destinationFile =
        projectRoot.resolve(confName).
          resolve(srcLanguage).
          resolve(relativeWithoutJava)

      file -> destinationFile
    }
  }

}

object Models
  extends Logger {

  def ofMoolRepository(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty).resolveConflicts
    val models = ofMoolRelCfgs(moolModel)
    Models(
      models = models,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }

  def ofMoolRepository2(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty)
    val topDown = runTopDown(moolModel)
    val models = topDown.map(d => d.rootPath -> Model.ofDescendState(d)).toMap

    Models(
      models = models,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }

  /**
    * Create a Model for each RelCfg.
    */
  def ofMoolRelCfgs(model: mool.Model): Map[mool.MoolPath, Model] = {
    for {
      (path, relCfg) <- model.relCfgs
      model <- Model.ofMoolRelCfg(model)(path, relCfg)
    } yield path -> model
  }

  def testBlds(moolModel: mool.Model)(path: mool.MoolPath): Map[mool.MoolPath, mool.Bld] = {
    moolModel.blds.filter {
      case (bldPath, bld) =>
        bld.rule_type.contains("test") &&
          moolModel.bldsToBlds.get(bldPath).exists(_.contains(path))
    }
  }

  case class DescendState(
    model: mool.Model,
    rootPath: MoolPath,
    root: mool.RelCfg,
    dependencies: Configurations[Dependency],
    parents: Set[MoolPath],
    children: Set[MoolPath]
  ) extends Logger {

    def isComplete = children.isEmpty

    def files(fs: FileSystem = FileSystems.getDefault): Set[Path] = {
      val mainRoot = fs.getPath("main")
      val testRoot = fs.getPath("test")

      val mainSrcs = dependencies.main.foldLeft(Set.empty[Path]) {
        case (accum, dep) =>
          dep match {
            case Dependency.Bld(bldPath) =>
              val bld = model.blds(bldPath)
              val srcPaths = for {
                srcPath <- bld.srcPaths(model, bldPath)
              } yield mainRoot.resolve(bld.language).resolve(srcPath)
              accum ++ srcPaths
            case _ => accum
          }
      }

      dependencies.test.foldLeft(mainSrcs) {
        case (accum, dep) =>
          dep match {
            case Dependency.Bld(bldPath) =>
              val bld = model.blds(bldPath)
              val srcPaths = for {
                srcPath <- bld.srcPaths(model, bldPath)
              } yield testRoot.resolve(bld.language).resolve(srcPath)
              accum ++ srcPaths
            case _ => accum
          }
      }
    }

    /**
      * The heuristic this implements for conflicts, is that the first RelCfg to reach
      * a Bld wins.
      */
    def goDown(siblings: Iterable[DescendState]): Iterable[DescendState] = {
      for {
        here <- children
      } yield {
        val children =
          (model.bldsToBlds(here) ++ model.bldsToCompileBlds(here)) --
            //prevent following dependency loops
            (parents + here)

        val testBldPaths =
          for {
            here <- children
            testBldPath <- model.bldsToTestBlds(here) -- (parents + here)
          } yield testBldPath

        val testDeps: Configurations[Dependency] =
          testBldPaths.foldLeft(dependencies) { (accum, child) =>
            val childBld = model.blds(child)
            val childDep = Dependency.ofBld(child, childBld)
            val mainProjectDep = {
              for {
                siblingMain <- siblings.find(_.dependencies.main.contains(childDep))
              } yield Configurations[Dependency](main = Set(mool.Dependency.RelCfg(siblingMain.rootPath)))
            }
            val testProjectDep = {
              for {
                siblingTest <- siblings.find(_.dependencies.test.contains(childDep))
              } yield Configurations[Dependency](test = Set(mool.Dependency.RelCfg(siblingTest.rootPath)))
            }

            mainProjectDep.orElse(testProjectDep) match {
              case None =>
                logger.trace(rootPath.mkString(".") + " -> " + childDep.shows)
                accum.withTests(childDep)
              case Some(sibling) =>
                //The Bld is already owned by a RelCfg, so depend on it instead.
                logger.trace(rootPath.mkString(".") + " -> " + sibling.shows)
                accum ++ sibling
            }
          }

        val deps =
          children.foldLeft(testDeps) { (accum, child) =>
            val childBld = model.blds(child)
            val childDep = Dependency.ofBld(child, childBld)
            val mainProjectDep = {
              for {
                siblingMain <- siblings.find(_.dependencies.main.contains(childDep))
              } yield Configurations[Dependency](main = Set(mool.Dependency.RelCfg(siblingMain.rootPath)))
            }
            val testProjectDep = {
              for {
                siblingTest <- siblings.find(_.dependencies.test.contains(childDep))
              } yield Configurations[Dependency](test = Set(mool.Dependency.RelCfg(siblingTest.rootPath)))
            }

            mainProjectDep.orElse(testProjectDep) match {
              case None =>
                logger.trace(rootPath.mkString(".") + " -> " + childDep.shows)
                accum.withMains(childDep)
              case Some(sibling) =>
                //The Bld is already owned by a RelCfg, so depend on it instead.
                logger.trace(rootPath.mkString(".") + " -> " + sibling.shows)
                accum ++ sibling
            }
          }

        copy(
          dependencies = deps,
          parents = parents + here,
          children = children
        )
      }
    }
  }

  def runTopDown(model: mool.Model): Vector[DescendState] = {
    val roots =
      for {
        (relCfgPath, relCfg) <- model.relCfgs
        if relCfg.`jar-with-dependencies`.nonEmpty || relCfg.`jar-no-dependencies`.nonEmpty
      } yield {
        val bldPath = relCfg.`jar-with-dependencies`.orElse(relCfg.`jar-no-dependencies`).get.targetPath

        DescendState(
          model = model,
          rootPath = relCfgPath,
          root = relCfg,
          dependencies = Configurations[Dependency](main = Set(Dependency.Bld(bldPath))),
          parents = Set.empty,
          children = Set(bldPath)
        )
      }

    def go(completes: Iterable[DescendState], incompletes: ParIterable[DescendState]): (Iterable[DescendState], ParIterable[DescendState]) = {
      val maybeCompletes =
        for {
          incomplete <- incompletes
          maybeComplete <- incomplete.goDown(completes ++ incompletes)
        } yield maybeComplete

      val (newCompletes, newIncompletes) = maybeCompletes.partition(_.isComplete)

      (completes ++ newCompletes, newIncompletes)
    }

    def run(complete: Iterable[DescendState], incomplete: ParIterable[DescendState]): Iterable[DescendState] = {
      if (incomplete.nonEmpty) {
        val (nextComplete, nextIncomplete) = go(complete, incomplete)
        run(nextComplete, nextIncomplete)
      } else complete
    }

    run(Iterable.empty, roots.par).toVector
  }

}
