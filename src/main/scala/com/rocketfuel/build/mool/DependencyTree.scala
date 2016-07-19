package com.rocketfuel.build.mool

import com.rocketfuel.build._
import java.nio.file.Path
import scalaz._
import scalaz.Scalaz._

sealed trait Dependency {
  override def toString: String = this.shows
}

object Dependency
  extends Logger {

  case class Bld(path: MoolPath) extends Dependency

  case class Maven(
    groupId: String,
    artifactId: String,
    version: String
  ) extends Dependency

  case class RelCfg(path: MoolPath) extends Dependency

  implicit val show: Show[Dependency] =
    new Show[Dependency] {
      override def show(f: Dependency): Cord = {
        f match {
          case Bld(path) =>
            if (path.nonEmpty)
              "Bld(Vector(\"" +: Cord.fromStrings(path.intersperse("\",\"")) :+ "\")"
            else "Bld(Vector())"
          case Maven(groupId, artifactId, version) =>
            "Maven(\"" +: Cord.fromStrings(Vector(groupId, artifactId, version).intersperse("\",\"")) :+ "\")"
          case RelCfg(path) =>
            if (path.nonEmpty)
              Cord.fromStrings("RelCfg(Vector(\"" +: path.intersperse("\",\"") :+ "\")")
            else "RelCfg(Vector())"
        }
      }
    }

  implicit val equal: Equal[Dependency] =
    Equal.equalA[Dependency]

  def ofBld(path: MoolPath, bld: mool.Bld): Dependency = {
    bld.maven_specs match {
      case None =>
        Bld(path)
      case Some(mvnSpecs) =>
        Maven(mvnSpecs.group_id, mvnSpecs.artifact_id, mvnSpecs.version)
    }
  }

  case class DescendState(
    model: Model,
    rootPath: MoolPath,
    root: mool.RelCfg,
    dependencies: Configurations[Dependency],
    parents: Set[MoolPath],
    children: Set[MoolPath]
  ) {

    def copies(destRoot: Path): Map[Path, Path] = {
      for {
        (configuration, dependency) <- Seq(("main", dependencies.main), ("test", dependencies.test))
        (src, dst) <-
          dependency match {
            case Dependency.Bld(bldPath) =>
              val bld = model.blds(bldPath)
              val srcLanguage =
                bld.rule_type match {
                  case "file_coll" =>
                    "resources"
                  case "java_proto_lib" =>
                    "proto"
                  case "java_lib" | "java_test" =>
                    "java"
                  case "scala_lib" | "scala_test" =>
                    "scala"
                }

              for (file <- bld.srcPaths(model, bldPath)) yield {
                val relative = model.root.relativize(file)
                val relativeWithoutJava = relative.subpath(1, relative.getNameCount)

                val destinationFile =
                  destRoot.resolve(configuration).
                    resolve(srcLanguage).
                    resolve(relativeWithoutJava)

                (file, destinationFile)
              }
            case _ =>
              Vector.empty
          }
      } yield src -> dst
    }

    /**
      * The heuristic this implements for conflicts, is that the first RelCfg to reach
      * a Bld wins.
      */
    def goDown(model: Model, siblings: Vector[DescendState]): Vector[DescendState] = {
      for {
        here <- children.toVector
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
              } yield Configurations[Dependency](main = Set(RelCfg(siblingMain.rootPath)))
            }
            val testProjectDep = {
              for {
                siblingTest <- siblings.find(_.dependencies.test.contains(childDep))
              } yield Configurations[Dependency](test = Set(RelCfg(siblingTest.rootPath)))
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
              } yield Configurations[Dependency](main = Set(RelCfg(siblingMain.rootPath)))
            }
            val testProjectDep = {
              for {
                siblingTest <- siblings.find(_.dependencies.test.contains(childDep))
              } yield Configurations[Dependency](test = Set(RelCfg(siblingTest.rootPath)))
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

  def runTopDown(model: Model): Vector[DescendState] = {
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
          dependencies = Configurations[Dependency](),
          parents = Set.empty,
          children = Set(bldPath)
        )
      }

    def go(states: Vector[DescendState]): Vector[DescendState] = {
      for {
        state <- states
        nextState <- state.goDown(model, states)
      } yield nextState
    }

    go(roots.toVector)
  }

  def relCfgDependencies(model: Model): Map[MoolPath, Set[Dependency]] = {
    val results = runTopDown(model)
    for {
      (relCfgPath, descStates) <- results.groupBy(_.rootPath)
    } yield {
      val deps = descStates.flatMap(_.dependencies.main).toSet
      relCfgPath -> deps
    }
  }

}

object DependencyTree {
  private def unfoldBlds(
    model: Model,
    blds: Map[MoolPath, Bld],
    maxDepth: Int
  ): Stream[Tree[Dependency]] = {
    /*
    These are the roots of the dependency tree creation. It's not enough
    to simply have the root nodes. There is also a set of parent nodes to
    prevent infinite loops.
     */
    val roots =
      blds.toStream.map(b => (Vector(b._1), b._2))

    val dependencies =
      for {
        (bldPath, bld) <- model.blds
      } yield bldPath -> Dependency.ofBld(bldPath, bld)

    Tree.unfoldForest[(Vector[MoolPath], Bld), Dependency](roots) {
      case (parents, hereBld: Bld) =>
        val herePath = parents.head

        val hereDep =
          dependencies(herePath)

        if (parents.size < maxDepth) {
          val deps =
            for {
              depPath <- model.bldsToBlds(herePath).toStream
              if !parents.contains(depPath)
            } yield {
              val depBld = model.blds(depPath)
              (depPath +: parents, depBld)
            }
          val compileDeps =
            for {
              depPath <- model.bldsToCompileBlds(herePath).toStream
              if !parents.contains(depPath)
            } yield {
              val depBld = model.blds(depPath)
              (depPath +: parents, depBld)
            }
          (hereDep, () => deps ++ compileDeps)
        } else (hereDep, () => Stream.Empty)
    }
  }

  def ofRelCfgs(
    model: Model,
    maxDepth: Int
  ): Stream[Tree[Dependency]] = {

    val bldPaths =
      for {
        (relCfgPath, relCfg) <- model.relCfgs
        artifact <- relCfg.`jar-with-dependencies`
      } yield {
        val bldPath = artifact.targetPath
        bldPath -> (relCfgPath, model.blds(bldPath))
      }

    val bldTree = unfoldBlds(model, bldPaths.map(kvp => kvp.copy(_2 = kvp._2._2)), maxDepth)

    bldTree.map {
      case child@Tree.Node(Dependency.Bld(path), forest) =>
        val (relCfgPath, _) = bldPaths(path)
        Tree.Node(Dependency.RelCfg(relCfgPath), Stream(child))
    }
  }

  def ofRootBlds(model: Model, maxDepth: Int): Stream[Tree[Dependency]] = {
    val rootBlds = model.blds.filter(! _._2.rule_type.contains("test")) -- model.bldsToBlds.values.flatten
    unfoldBlds(model, rootBlds, maxDepth)
  }

  def ofTestBlds(model: Model, maxDepth: Int): Stream[Tree[Dependency]] = {
    val testRoots = model.blds.filter(_._2.rule_type.contains("test")) -- model.bldsToBlds.values.flatten
    unfoldBlds(model, testRoots, maxDepth)
  }

}
