package com.rocketfuel.graphviz

import java.nio.file.{Files, Path}
import java.util

/*
http://www.graphviz.org/content/dot-language
 */

/**
  *
  * @param nodes nodes that aren't connected to anything
  * @param clusters nodes that are within a group
  * @param edges nodes that are connected but not in a group
  */
case class Graph(
  nodes: Set[Node],
  clusters: Set[Cluster],
  edges: Set[Edge]
) {
  def toDot: String = {
    val sb = new StringBuilder()

    sb.append(Graph.start)

    for (node <- nodes) {
      node.toDot(1, sb)
    }

    clusters.foreach(c => c.toDot(1, sb))

    edges.foreach(_.toDot(1, sb))

    sb.append(Graph.end)

    sb.toString
  }

  def writeDot(file: Path): Unit = {
    Option(file.getParent).foreach(Files.createDirectories(_))
    val out = Files.newBufferedWriter(file)
    try out.write(toDot)
    finally out.close()
  }
}

object Graph {
  val start = "digraph mool {\n  rankdir=LR\n  node [fontname = \"Liberation Mono:style=Regular\"]\n"

  val end = '}'
}

case class Cluster(
  namePostfix: String,
  edges: Set[Edge]
) {
  val name = "cluster_" + namePostfix

  def toDot(indent: Int, sb: StringBuilder): Unit = {
    sb.appendIndented(indent, "subgraph ")
    sb.append(name.dotQuote)
    sb.append(" {\n")
    edges.foreach(_.toDot(indent + 1, sb))
    sb.appendIndented(indent, "}\n")
  }
}

case class Edge(
  left: String,
  right: String,
  label: Option[String]
) {
  def toDot(indent: Int, sb: StringBuilder): Unit = {
    sb.appendIndented(indent, left.dotQuote)
    sb.append(" -> ")
    sb.append(right.dotQuote)
    for (l <- label) {
      sb.append(" [label = ")
      sb.append(l.dotQuote)
      sb.append(']')
    }
    sb.append("\n")
  }

  override def hashCode(): Int =
    util.Arrays.hashCode(Array[Object](left, right))

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: Edge =>
        left == other.left && right == other.right
      case _ =>
        false
    }
  }
}

case class Node(
  name: String,
  attributes: Map[String, String] = Map.empty
) {

  def toDot(indent: Int, sb: StringBuilder): Unit = {
    sb.appendIndented(indent, name.dotQuote)
    if (attributes.nonEmpty) {
      sb.append(" [")
      for ((name, value) <- attributes) {
        sb.append(name)
        sb.append('=')
        sb.append(value)
        sb.append(',')
      }
      sb.deleteCharAt(sb.length - 1)
      sb.append(']')
    }
    sb.append('\n')
  }

  override def hashCode(): Int = name.hashCode

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case that: Node =>
        this.name == that.name
      case _ =>
        false
    }
}