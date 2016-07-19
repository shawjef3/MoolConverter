package com.rocketfuel.build.mool

import com.rocketfuel.graphviz.{Cluster, Edge, Graph}
import java.nio.file.{Files, Path}
import org.apache.commons.io.IOUtils

trait Graphviz {
  self: Model =>

  /**
    * Blds that don't belong to any RelCfg
    * @return
    */
  def nodes: Set[String] = {
    for {
      bldPath <- bldOrphans ++ testBldOrphans
    } yield bldPath.mkString(".")
  }

  def clusters: Set[Cluster] = {
    for {
      (relCfg, blds) <- relCfgsToBldsTransitive -- bldConflicts.keys
    } yield {
      val edges = for {
        (src, dsts) <- bldsToBlds.filterKeys(blds.contains).toVector
        srcString = src.mkString(".")
        dst <- dsts
      } yield Edge(srcString, dst.mkString("."), None)

      Cluster(relCfg.mkString("."), edges.toSet)
    }
  } toSet

  def edges: Set[Edge] =
    for {
      bldPath <- bldOrphans ++ testBldOrphans
      bldPathString = bldPath.mkString(".")
      targets = bldsToBlds(bldPath)
      target <- targets
    } yield Edge(bldPathString, target.mkString("."), None)

  def toGraph: Graph =
    Graph(
      nodes = nodes,
      clusters = clusters,
      edges = edges
    )

  def toRelCfgGraphs: Map[MoolPath, Graph] =
    for {
      (relCfgPath, relCfg) <- relCfgs
    } yield {
      val blds = relCfgsToBldsTransitive(relCfgPath)
      val depEdges = for {
        bld <- blds.toVector
        dst <- bldsToBlds(bld)
      } yield Edge(bld.mkString("."), dst.mkString("."), None)
      val compileDepEdges = for {
        bld <- blds.toVector
        dst <- bldsToCompileBldsTransitive(bld)
      } yield Edge(bld.mkString("."), dst.mkString("."), Graphviz.compile)
      relCfgPath ->
        Graph(Set.empty, Set.empty, (depEdges ++ compileDepEdges).toSet)
    }

  def writeRelCfgGraphs(dir: Path): Vector[Path] = {
    Files.createDirectories(dir)
    for ((relCfgPath, graph) <- toRelCfgGraphs) yield {
      val outFile = dir.resolve(relCfgPath.mkString(".") + ".dot")
      val out = Files.newBufferedWriter(outFile)
      try out.write(graph.toDot)
      finally IOUtils.closeQuietly(out)
      outFile
    }
  } toVector

  def renderRelCfgGraphCommands(dir: Path, layout: String, format: String): Vector[String] = {
    for {
      relCfgDot <- writeRelCfgGraphs(dir)
    } yield {
      val relCfgPng = relCfgDot.getParent.resolve(relCfgDot.getFileName.toString.dropRight(4) + s"-$layout.$format")
      s"dot $relCfgDot -T$format -K$layout -o$relCfgPng"
    }
  }

}

object Graphviz {
  object Layout {
    val Circo = "circo"
  }

  val compile = Some("compile")
}
