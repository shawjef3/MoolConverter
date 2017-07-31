package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn.Parents
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainBuildFiles extends App {

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/mool_conversion")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    Convert.buildFiles(destinationRoot)
  }

  Parents.writeRoot(destinationRoot)

}
