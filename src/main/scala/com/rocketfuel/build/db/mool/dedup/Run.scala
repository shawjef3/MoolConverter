package com.rocketfuel.build.db.mool.dedup

import com.rocketfuel.sdbc.PostgreSql._
import org.postgresql.util.PSQLException

object Run {

  val queries = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream("run.sql"))
    val queryTexts = source.mkString.split(';')
    queryTexts.map(Ignore(_))
  }

  def run()(implicit connection: Connection): Unit =
    for (query <- queries) {
      try query.ignore()
      catch {
        case e: PSQLException =>
          println(s"query failed: $query")
          throw e
      }
    }

}
