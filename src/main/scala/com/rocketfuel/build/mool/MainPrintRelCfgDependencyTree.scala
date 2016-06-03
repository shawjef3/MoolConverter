package com.rocketfuel.build.mool

import java.nio.file.Paths

/**
  * requires a large maximum stack size, e.g. -Xss515m
  */
object MainPrintRelCfgDependencyTree extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val model = Model.ofRepository(moolRoot)

  val depTree = DependencyTree.ofRelCfgs(model)

  val trees = depTree.map(_.map(_._2)).map(_.drawTree)

  trees.foreach(println)

}
