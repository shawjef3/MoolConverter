package com.rocketfuel.build.mool

import com.rocketfuel.graphviz.{Cluster, Edge, Graph, Node}
import java.nio.file.{Files, Path}
import org.apache.commons.io.IOUtils
import scala.sys.process._

trait Graphviz {
  self: Model =>

  /**
    * Blds that don't belong to any RelCfg
    * @return
    */
  def nodes: Set[Node] = {
    for {
      bldPath <- bldOrphans ++ testBldOrphans
    } yield Node(bldPath.mkString("."))
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

  private implicit class IsLocal(bldPath: MoolPath) {
    def isLocal: Boolean = {
      !bldPath.startsWith(Vector("java", "mvn"))
    }
  }

  def toRelCfgGraphs: Map[MoolPath, Graph] =
    for {
      (relCfgPath, relCfg) <- relCfgs
    } yield {
      val localBlds = relCfgsToBldsTransitive(relCfgPath).filter(_.isLocal)
      val depEdges = for {
        bld <- localBlds.toVector
        dst <- bldsToBlds(bld).filter(_.isLocal)
      } yield Edge(bld.mkString("."), dst.mkString("."), None)
      val root = for {
        rootBldPath <- relCfgsToBld(relCfgPath)
      } yield Node(rootBldPath.mkString("."), Map("color" -> "red", "shape" -> "box"))
      relCfgPath ->
        Graph(root.toSet, Set.empty, depEdges.toSet)
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

  def renderRelCfgGraphs(dir: Path, format: String): Unit = {
    for (relCfgDot <- writeRelCfgGraphs(dir)) {
      s"dot -T$format -O $relCfgDot".!
    }
  }

}
