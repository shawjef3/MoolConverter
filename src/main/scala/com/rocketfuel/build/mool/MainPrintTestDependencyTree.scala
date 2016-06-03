package com.rocketfuel.build.mool

object MainPrintTestDependencyTree extends App {

  import java.nio.file.Paths

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val model = Model.ofRepository(moolRoot)

  val depTrees = DependencyTree.ofTestBlds(model)

  val trees =
    depTrees.map(_.map(_._2).drawTree)

  trees.foreach(println)

}
