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
    for (copy <- Copy.all.iterator()) {
      val source = moolRoot.resolve(copy.source)
      val destination = destinationRoot.resolve(copy.destination)
      println(copy)
      if (dry) {
        assert(Files.exists(source))
      } else {
        Files.copy(source, destination)
      }
    }

    for (bld <- mool.Bld.localBlds.iterator()) {
      val identifier = mvn.Identifier.selectByBldId.on("bldId" -> bld.id).one()
      val dependencies = mvn.Dependency.selectBySourceId.on("sourceId" -> bld.id).vector()
      println(identifier)

      for (dependency <- dependencies) {
        print("\t")
        println(dependency)
      }

      val path = ModulePath.selectById(bld.id).get
      val pom = bld.pom(identifier, dependencies)

      if (!dry)
        Files.write(destinationRoot.resolve(path.path).resolve("pom.xml"), pom.toString.getBytes)
    }

  }

}
