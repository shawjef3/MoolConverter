package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn._
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainConvert extends App {

  val dry = false

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  val pomsPath = destinationRoot.resolve("parents")

  //TODO: delete destinationRoot
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

    val modulePaths = {
      for (ModulePath(id, path) <- ModulePath.list.iterator()) yield
        id -> path
    }.toMap

    val identifiers = {
      for (i <- mvn.Identifier.list.iterator()) yield {
        i.bldId -> i
      }
    }.toMap

    val dependencies =
      mvn.Dependency.list.vector().groupBy(_.sourceId)

    val localBlds = mool.Bld.localBlds.vector()

    for (bld <- localBlds) {
      val identifier = identifiers(bld.id)
      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)

      val path = modulePaths(bld.id)
      val modulePath = destinationRoot.resolve(path)
      val pom = bld.pom(identifier, bldDependencies, destinationRoot, modulePath)
      val pomPath = modulePath.resolve("pom.xml")

      if (dry) {
        println(identifier)

        for (dependency <- bldDependencies) {
          print("\t")
          println(dependency)
        }
      } else {
        Files.createDirectories(modulePath)
        Files.write(pomPath, pom.toString.getBytes)
      }
    }

    if (!dry) {
      Parents.writeRoot(destinationRoot)
      Parents.writeCheckStyle(destinationRoot)
      Parents.`Scala-common`.write(destinationRoot, Set())

      val parentPoms =
        localBlds.foldLeft(Parents.Poms.Empty) {
          case (poms, bld) =>
            val moduleRoot = modulePaths(bld.id)
            poms.add(bld, moduleRoot)
        }

      parentPoms.write(destinationRoot)
    }
  }

}
