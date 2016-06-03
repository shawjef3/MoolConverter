package com.rocketfuel.build.mool

/**
  * requires a large maximum stack size, e.g. -Xss515m
  */
object MainPrintRelCfgDependencyTree extends ModelApp {

  val depTree = DependencyTree.ofRelCfgs(model)

  val trees = depTree.map(_.map(_._2)).map(_.drawTree)

  trees.foreach(println)

}
