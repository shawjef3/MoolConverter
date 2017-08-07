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
    version: String,
    scope: String = "compile"
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
          case Maven(groupId, artifactId, version, scope) =>
            "Maven(\"" +: Cord.fromStrings(Vector(groupId, artifactId, version, scope).intersperse("\",\"")) :+ "\")"
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

}

object DependencyTree {
  private def unfoldBlds(
    model: Model,
    blds: Map[MoolPath, Bld],
    maxDepth: Int
  ): Vector[StrictTree[Dependency]] = {
    /*
    These are the roots of the dependency tree creation. It's not enough
    to simply have the root nodes. There is also a set of parent nodes to
    prevent infinite loops.
     */
    val roots =
      blds.toVector.map(b => (Vector(b._1), b._2))

    val dependencies =
      for {
        (bldPath, bld) <- model.blds
      } yield bldPath -> Dependency.ofBld(bldPath, bld)

    StrictTree.unfoldForest[(Vector[MoolPath], Bld), Dependency](roots) {
      case (parents, hereBld: Bld) =>
        val herePath = parents.head

        val hereDep =
          dependencies(herePath)

        if (parents.size < maxDepth) {
          val deps =
            for {
              depPath <- model.bldsToBlds(herePath).toVector
              if !parents.contains(depPath)
            } yield {
              val depBld = model.blds(depPath)
              (depPath +: parents, depBld)
            }
          (hereDep, deps)
        } else (hereDep, Vector.empty)
    }
  }

  def ofRelCfgs(
    model: Model,
    maxDepth: Int
  ): Vector[StrictTree[Dependency]] = {

    val bldPaths =
      for {
        (relCfgPath, relCfg) <- model.relCfgs
        artifact <- List(relCfg.`jar-with-dependencies`, relCfg.deploy).flatten
      } yield {
        val bldPath = artifact.targetPath
        bldPath -> (relCfgPath, model.blds(bldPath))
      }

    val bldTree = unfoldBlds(model, bldPaths.map(kvp => kvp.copy(_2 = kvp._2._2)), maxDepth)

    bldTree.map {
      case child@StrictTree(Dependency.Bld(path), forest) =>
        val (relCfgPath, _) = bldPaths(path)
        StrictTree(Dependency.RelCfg(relCfgPath), Vector(child))
    }
  }

  def ofRootBlds(model: Model, maxDepth: Int): Vector[StrictTree[Dependency]] = {
    val rootBlds = model.blds.filter(! _._2.rule_type.contains("test")) -- model.bldsToBlds.values.flatten
    unfoldBlds(model, rootBlds, maxDepth)
  }

  def ofTestBlds(model: Model, maxDepth: Int): Vector[StrictTree[Dependency]] = {
    val testRoots = model.blds.filter(_._2.rule_type.contains("test")) -- model.bldsToBlds.values.flatten
    unfoldBlds(model, testRoots, maxDepth)
  }

}
