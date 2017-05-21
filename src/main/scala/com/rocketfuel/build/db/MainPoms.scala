package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn.Parents
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainPoms extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/jshaw")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    Convert.poms(destinationRoot)
  }

  Parents.writeRoot(destinationRoot)

}
