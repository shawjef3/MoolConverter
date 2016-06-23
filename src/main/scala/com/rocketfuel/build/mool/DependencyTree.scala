package com.rocketfuel.build.mool

import scalaz._

sealed trait Dependency

object Dependency {

  case class Bld(path: MoolPath) extends Dependency

  case class Maven(
    groupId: String,
    artifactId: String,
    version: String
  ) extends Dependency

  implicit val show: Show[Dependency] =
    new Show[Dependency] {
      override def shows(f: Dependency): String = {
        f match {
          case Bld(path) =>
            "Bld(" + path.mkString(".") + ")"
          case r: Maven =>
            r.toString
        }
      }
    }

  def ofBld(path: MoolPath, bld: com.rocketfuel.build.mool.Bld): Dependency = {
    bld.maven_specs match {
      case None =>
        Bld(path)
      case Some(mvnSpecs) =>
        Maven(mvnSpecs.group_id, mvnSpecs.artifact_id, mvnSpecs.version)
    }
  }

}

object DependencyTree {
  private def unfoldBlds(model: Model, blds: Map[MoolPath, Bld]): Vector[StrictTree[(Vector[Dependency], Dependency)]] = {
    val roots =
      blds.map(b => Dependency.ofBld(b._1, b._2))

    val rootsWithSelves =
      roots.map(root => (Vector[Dependency](root), root))

    StrictTree.unfoldForest[(Vector[Dependency], Dependency), (Vector[Dependency], Dependency)](rootsWithSelves.toVector) {
      case (parents, d: Dependency.Bld) =>
        val children =
          for {
            bldPath <- model.bldsToBlds(d.path).toVector
            bld = model.blds(bldPath)
            bldDependency = Dependency.ofBld(bldPath, bld)
            if !parents.contains(bldDependency)
          } yield {
            (parents :+ bldDependency, bldDependency)
          }
        ((parents, d), children)
      case (parents, r: Dependency.Maven) =>
        ((parents, r), Vector.empty)
      case _ =>
        throw new IllegalArgumentException("BLD dependent on a RelCfg")
    }
  }

  def ofBlds(model: Model): Vector[StrictTree[(Vector[Dependency], Dependency)]] = {
    val nonTestRoots = model.blds.filter(! _._2.rule_type.contains("test"))

    unfoldBlds(model, nonTestRoots)
  }

  def ofTestBlds(model: Model): Vector[StrictTree[(Vector[Dependency], Dependency)]] = {
    val testRoots = model.blds.filter(_._2.rule_type.contains("test"))

    unfoldBlds(model, testRoots)
  }

  def gorns(StrictTree: StrictTree[(Vector[Dependency], Dependency)]): Set[Vector[Dependency]] = {
    StrictTree.foldRight[Set[Vector[Dependency]]](Set.empty) {
      case ((parentsAndHere, here), accum) =>
        val parentGorn = parentsAndHere.dropRight(1)
        (accum - parentGorn) + parentsAndHere
    }
  }

}
