package com.rocketfuel.build.db

import com.rocketfuel.build.db.mvn._
import com.rocketfuel.sdbc.PostgreSql._
import com.zaxxer.hikari.HikariConfig
import java.nio.file._

object MainConvert extends App {

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val destinationRoot = Paths.get("/tmp").resolve("mool-conversion")

  val pomsPath = destinationRoot.resolve("parents")

  //TODO: delete destinationRoot
  val dbConfig = new HikariConfig()

  dbConfig.setJdbcUrl("jdbc:postgresql://localhost/jshaw")

  val pool = Pool(dbConfig)

  pool.withConnection { implicit connection =>
    val copies = Copy.all.vector().toSet
    val fileCopier = FileCopier(copies, moolRoot, destinationRoot)
    fileCopier.copyAll()

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

      Files.createDirectories(modulePath)
      Files.write(pomPath, pom.toString.getBytes)
    }

    Parents.writeRoot(destinationRoot)
    Parents.writeCheckStyle(destinationRoot)
    Parents.`Scala-common`.write(destinationRoot, Set())
    Parents.`Scala-2.11`.write(destinationRoot, Set())
    Parents.`Scala-2.12`.write(destinationRoot, Set())

    val parentPoms =
      localBlds.foldLeft(Parents.Poms.Empty) {
        case (poms, bld) =>
          val moduleRoot = modulePaths(bld.id)
          poms.add(bld, moduleRoot)
      }

    parentPoms.write(destinationRoot)
  }

}
