package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn.{DependencySupplements, Exclusion}
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file.Paths

object MainDeploy extends App {

  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/mool_conversion")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>

    Deploy.undeploy()

    Deploy.deploy()

    val moolRoot = Paths.get("/tmp/vostok")

    Clone.vostok(moolRoot)

    val moolModel = com.rocketfuel.build.mool.Model.ofRepository(moolRoot)

    val sqlModel = new com.rocketfuel.build.db.mool.Model(moolModel)

    sqlModel.insert()

    Hacks.hack()

    mool.dedup.Run.run()

    DependencySupplements.supplement()

    Exclusion.run()
  }

}
