package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainConvert extends App {

  val moolRoot = Paths.get("/tmp/vostok")

  val destinationRoot = Paths.get("/tmp/mool-conversion")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/mool_conversion")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    Convert.files(moolRoot, destinationRoot)
    Convert.testFiles(moolRoot, destinationRoot)

    Convert.poms(destinationRoot)
  }

}
