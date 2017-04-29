package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainConvert extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  val pomsPath = destinationRoot.resolve("parents")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/jshaw")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    Convert.files(moolRoot, destinationRoot)

    Convert.poms(destinationRoot)
  }

  Convert.gridModeling(destinationRoot)

}
