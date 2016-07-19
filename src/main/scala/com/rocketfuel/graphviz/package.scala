package com.rocketfuel

package object graphviz {

  type NodeId = String

  class DotQuote(val s: String) extends AnyVal {
    def dotQuote: String = {
      val escaped =
        s.flatMap {
          case '"' => Vector('\\', '"')
          case otherwise => Vector(otherwise)
        }
      '"' + escaped + '"'
    }
  }

  implicit def toDotQuote(s: String): DotQuote =
    new DotQuote(s)

  class AppendIndented(val sb: StringBuilder) extends AnyVal {
    def appendIndented(indent: Int, toAppend: String): Unit = {
      for (i <- 0 until (indent * 2)) sb.append(' ')
      sb.append(toAppend)
    }
  }

  implicit def toAppendIndented(sb: StringBuilder): AppendIndented =
    new AppendIndented(sb)

}
