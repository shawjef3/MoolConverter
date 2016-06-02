package com.rocketfuel.build.mool

import java.nio.file.Paths
import scalaz.std.vector._

object MainPrintBldDependencyTree extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val model = Model.ofRepository(moolRoot)

  val depTrees = DependencyTree.bldRoots(model)

  lazy val gorns = depTrees.map(DependencyTree.gorns).map(_.toString)

  lazy val trees =
    depTrees.map(_.map(_._2).drawTree)

  gorns.foreach(println)

//  depTrees.map(_.map(_._1).drawTree).foreach(println)

}
