package com.rocketfuel.build.mool

import scalaz._, Scalaz._

object MainPrintTestDependencyTree extends ModelApp {

  val depTrees = DependencyTree.ofTestBlds(model, 2)

  val trees =
    depTrees.map(_.drawTree)

  trees.foreach(println)

}

object MainPrintBldDependencyTree extends ModelApp {

  val depTrees = DependencyTree.ofRootBlds(model, 2)

  val trees =
    depTrees.map(_.drawTree)

  trees.foreach(println)

}

object MainPrintRelCfgDependencyTree extends ModelApp {
  val depTrees = DependencyTree.ofRelCfgs(model, 2)

  val trees = depTrees.map(_.drawTree)

  trees.foreach(println)
}

object MainPrintUnresolvedTests extends ModelApp {

  val testBlds = DependencyTree.ofTestBlds(model, 2)

  testBlds.filter {
    case n@Tree.Node(Dependency.Bld(path), children) =>
      val main: Tree[Dependency] = Tree.Leaf(Dependency.Bld(path.init :+ path.last.dropRight(4)))
      ! children.exists(d => d === main)
    case _ =>
      false
  } foreach(d => println(d.drawTree))

}
