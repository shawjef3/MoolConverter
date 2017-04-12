package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.db._
import com.rocketfuel.build.mool
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file.Paths

object MainInsert extends App {

  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/jshaw")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>

    Deploy.undeploy()

    Deploy.deploy()

    val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

    val moolModel = mool.Model.ofRepository(moolRoot, Map.empty)

    com.rocketfuel.build.db.mool.Model.insert(moolModel)
  }

}
