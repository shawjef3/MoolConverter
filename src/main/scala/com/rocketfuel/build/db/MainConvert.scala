package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn._
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainConvert extends App {

  val dry = true

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  if (!dry) {
    if (Files.exists(destinationRoot))
      Files.walk(destinationRoot).forEach(p => Files.delete(p))
  }

  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/jshaw")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    for (copy <- Copy.all.iterator()) {
      val source = moolRoot.resolve(copy.source)
      val destination = destinationRoot.resolve(copy.destination)
      if (dry) {
        println(copy)
        assert(Files.exists(source))
      } else {
        Files.createDirectories(destination.getParent)
        Files.copy(source, destination)
      }
    }

    for (bld <- mool.Bld.localBlds.iterator()) {
      val identifier = mvn.Identifier.selectByBldId.on("bldId" -> bld.id).one()
      val dependencies = mvn.Dependency.selectBySourceId.on("sourceId" -> bld.id).vector()

      val path = ModulePath.selectById(bld.id).get
      val pom = bld.pom(identifier, dependencies)
      val pomPath = destinationRoot.resolve(path.path).resolve("pom.xml")

      if (dry) {
        println(identifier)

        for (dependency <- dependencies) {
          print("\t")
          println(dependency)
        }
      } else {
        Files.createDirectories(pomPath.getParent)
        Files.write(pomPath, pom.toString.getBytes)
      }
    }

  }

}
