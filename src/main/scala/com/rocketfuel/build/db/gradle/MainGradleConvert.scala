package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.db.Convert
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig

object MainGradleConvert extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = moolRoot.resolve("projects")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/mool_conversion")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
//    GradleConvert.files(moolRoot, destinationRoot)
//    GradleConvert.rootBuildFiles(moolRoot)
//    GradleConvert.builds(moolRoot, destinationRoot)
    Convert.files(moolRoot, destinationRoot)
//    SimpleGradleConvert.rootBuildFiles(moolRoot)
    SimpleGradleConvert.builds(moolRoot, destinationRoot)
  }

  Convert.gridModeling(destinationRoot)

}
