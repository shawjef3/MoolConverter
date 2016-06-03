package com.rocketfuel.build.mool

object MainPrintTestDependencyTree extends ModelApp {

  val depTrees = DependencyTree.ofTestBlds(model)

  val trees =
    depTrees.map(_.map(_._2).drawTree)

  trees.foreach(println)

}
