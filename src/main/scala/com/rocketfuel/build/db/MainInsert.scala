package com.rocketfuel.build.db

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

    val moolModel = com.rocketfuel.build.mool.Model.ofRepository(moolRoot)

    val sqlModel = new com.rocketfuel.build.db.mool.Model(moolModel)

    sqlModel.insert()
  }

}
