package com.rocketfuel.build.mool

import scalaz._

sealed trait Dependency

import scalaz.Show

object Dependency {

  case class Bld(path: MoolPath) extends Dependency

  case class RelCfg(path: MoolPath) extends Dependency

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
          case RelCfg(path) =>
            "RelCfg(" + path.mkString(".") + ")"
          case r: Maven =>
            r.toString
        }
      }
    }

  def ofBld(path: Vector[String], bld: com.rocketfuel.build.mool.Bld): Dependency = {
    bld.maven_specs match {
      case None =>
        Bld(path)
      case Some(mvnSpecs) =>
        Maven(mvnSpecs.group_id, mvnSpecs.artifact_id, mvnSpecs.version)
    }
  }

}

object DependencyTree {
  def ofRelCfgs(model: Model): Stream[Tree[(Vector[Dependency], Dependency)]] = {
    val roots =
      model.relCfgs.keys.toStream.map(Dependency.RelCfg)

    val rootsWithSelves =
      roots.map(root => (Vector[Dependency](root), root))

    Tree.unfoldForest[(Vector[Dependency], Dependency), (Vector[Dependency], Dependency)](rootsWithSelves) {
      case (parents, d: Dependency.RelCfg) =>
        lazy val children =
          for {
            bldPath <- model.relCfgsToBld.get(d.path).flatten.toStream
            bld = model.blds(bldPath)
            bldDependency = Dependency.ofBld(bldPath, bld)
            if ! parents.contains(bldDependency)
          } yield {
            (parents :+ bldDependency, bldDependency)
          }
        ((parents, d), () => children)
      case (parents, d: Dependency.Bld) =>
        lazy val children =
          for {
            bldPath <- model.bldsToBlds(d.path).toStream
            bld = model.blds(bldPath)
            bldDependency = Dependency.ofBld(bldPath, bld)
            if ! parents.contains(bldDependency)
          } yield {
            (parents :+ bldDependency, bldDependency)
          }
        ((parents, d), () => children)
      case (parents, r: Dependency.Maven) =>
        ((parents, r), () => Stream.empty)
    }
  }

  private def unfoldBlds(model: Model, blds: Map[Vector[String], Bld]): Stream[Tree[(Vector[Dependency], Dependency)]] = {
    val roots =
      blds.map(b => Dependency.ofBld(b._1, b._2))

    val rootsWithSelves =
      roots.map(root => (Vector[Dependency](root), root))

    Tree.unfoldForest[(Vector[Dependency], Dependency), (Vector[Dependency], Dependency)](rootsWithSelves.toStream) {
      case (parents, d: Dependency.Bld) =>
        lazy val children =
          for {
            bldPath <- model.bldsToBlds(d.path).toStream
            bld = model.blds(bldPath)
            bldDependency = Dependency.ofBld(bldPath, bld)
            if !parents.contains(bldDependency)
          } yield {
            (parents :+ bldDependency, bldDependency)
          }
        ((parents, d), () => children)
      case (parents, r: Dependency.Maven) =>
        ((parents, r), () => Stream.empty)
      case _ =>
        throw new IllegalArgumentException("BLD dependent on a RelCfg")
    }
  }

  def ofBlds(model: Model): Stream[Tree[(Vector[Dependency], Dependency)]] = {
    val nonTestRoots = model.blds.filter(! _._2.rule_type.contains("test"))

    unfoldBlds(model, nonTestRoots)
  }

  def ofTestBlds(model: Model): Stream[Tree[(Vector[Dependency], Dependency)]] = {
    val testRoots = model.blds.filter(_._2.rule_type.contains("test"))

    unfoldBlds(model, testRoots)
  }

  def gorns(tree: Tree[(Vector[Dependency], Dependency)]): Set[Vector[Dependency]] = {
    tree.foldRight[Set[Vector[Dependency]]](Set.empty) {
      case ((parentsAndHere, here), accum) =>
        val parentGorn = parentsAndHere.dropRight(1)
        (accum - parentGorn) + parentsAndHere
    }
  }

}
