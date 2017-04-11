package com.rocketfuel.graphviz

import org.scalatest.FunSuite

class GraphSpec extends FunSuite {

  test("empty dot") {
    val g = Graph(Set.empty, Set.empty, Set.empty)
    val dot = g.toDot

    assertResult(Graph.start + Graph.end)(dot)
  }

  test("one edge") {
    val g = Graph(Set.empty, Set.empty, Set(Edge("hi", "bye", None)))
    val dot = g.toDot

    assertResult(Graph.start + "  \"hi\" -> \"bye\"\n" + Graph.end)(dot)
  }

  test("one cluster") {
    val g = Graph(Set.empty, Set(Cluster("name", Set(Edge("hi", "bye", None)))), Set.empty)
    val dot = g.toDot

    assertResult(
      s"""${Graph.start}  subgraph "cluster_name" {
        |    "hi" -> "bye"
        |  }
        |}""".stripMargin
    )(dot)
  }

  test("one cluster and one edge") {
    val g = Graph(Set.empty, Set(Cluster("name", Set(Edge("hi", "bye", None)))), Set(Edge("a", "b", None)))
    val dot = g.toDot

    assertResult(
      s"""${Graph.start}  subgraph "cluster_name" {
        |    "hi" -> "bye"
        |  }
        |  "a" -> "b"
        |}""".stripMargin
    )(dot)
  }

  test("quoting and escaping") {
    assertResult("\"hi\"")("hi".dotQuote)
    assertResult("\"\\\"\"")("\"".dotQuote)
  }

}
