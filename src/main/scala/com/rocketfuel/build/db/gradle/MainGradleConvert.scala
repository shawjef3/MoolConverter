package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.db.Convert
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig

object MainGradleConvert extends App {

  // val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")
  val moolRoot = Paths.get("/tmp/vostok")

  val destinationRoot = Paths.get("/tmp/gradle-conversion")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/mool_conversion")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    Convert.files(moolRoot, destinationRoot.resolve("projects"))

    SimpleGradleConvert.builds(moolRoot, destinationRoot)
  }

  // Convert.gridModeling(destinationRoot)

}
