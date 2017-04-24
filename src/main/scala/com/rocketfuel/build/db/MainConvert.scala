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

  Parents.write(pomsPath)

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
      val pom = bld.pom(bld, identifier, bldDependencies, destinationRoot, modulePath)
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

    val aggregatePom =
      <project xmlns="http://maven.apache.org/POM/4.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.rocketfuel</groupId>
        <artifactId>aggregate</artifactId>
        <version>9.0.0-SNAPSHOT</version>
        <packaging>pom</packaging>

        <properties>
          <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
          <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        </properties>

        <modules>
          {
          Parents.modules
          }
          {
          for (bld <- localBlds) yield {
            val path = modulePaths(bld.id)
            <module>
              {path}
            </module>
          }
          }
        </modules>
      </project>

    val pomPath =
      destinationRoot.resolve("pom.xml")

    Files.write(pomPath, aggregatePom.toString.getBytes)

  }

}
