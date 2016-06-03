package com.rocketfuel.build.mool

import java.nio.file.Paths

object MainPrintBldDependencyTree extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val model = Model.ofRepository(moolRoot)

  val depTrees = DependencyTree.ofBlds(model)

  val trees =
    depTrees.map(_.map(_._2).drawTree)

  trees.foreach(println)

}
