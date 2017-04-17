package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn._
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainConvert extends App {

  val dry = true

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/jshaw")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    for (copy <- Copy.list.iterator()) {
      println(copy)
      if (dry) {
        assert(Files.exists(moolRoot.resolve(copy.source)))
      } else {
        Files.copy(moolRoot.resolve(copy.source), destinationRoot.resolve(copy.destination))
      }
    }

    for (identifier <- Identifier.list.iterator()) {
      println(identifier)
      for (dependency <- Dependency.selectBySourceId.on("sourceId" -> identifier.bldId).iterator()) {
        println(s"\t${identifier.bldId} -> $dependency")
      }
    }

  }

}
