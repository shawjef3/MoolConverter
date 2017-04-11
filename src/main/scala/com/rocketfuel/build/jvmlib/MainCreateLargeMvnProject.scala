package com.rocketfuel.build.jvmlib

import java.nio.file.{Files, Paths}

object MainCreateLargeMvnProject extends App {

  val n = 4000

  val sourcesPerModule = 11

  val root = Paths.get("/tmp").resolve("maven-large")

  Files.createDirectories(root)

  val aggregatePomPath = root.resolve("pom.xml")

  val aggregate = {
    <groupId>com.rocketfuel</groupId>
    <artifactId>aggregate</artifactId>
    <version>9.0.0-SNAPSHOT</version>
  }

  val aggregatePom =
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      {aggregate}

      <packaging>pom</packaging>

      <modules>
        {
        for (i <- 0 until n) yield
          <module>
            {i}
          </module>
        }
      </modules>

    </project>

  val aggregateSbtPath =
    root.resolve("build.sbt")

  val aggregateSbt = {
    val projects = {
      for (i <- 0 until n) yield
        s"""lazy val p$i = project.in(file("$i"))"""
    }.mkString("\n")

    val aggregate = {
      for (i <- 0 until n) yield
        s"p$i"
    }.mkString(",\n")

    s"""project.in(file(".")).aggregate($aggregate)
       |$projects
     """.stripMargin
  }

  Files.write(aggregatePomPath, aggregatePom.toString.getBytes)

  Files.write(aggregateSbtPath, aggregateSbt.getBytes)

  for (i <- 0 until n) {

    val moduleRoot = root.resolve(i.toString)

    Files.createDirectories(moduleRoot)

    val pomPath = moduleRoot.resolve("pom.xml")

    val pom =
      <project xmlns="http://maven.apache.org/POM/4.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <parent>
          {aggregate}
        </parent>

        <groupId>com.rocketfuel</groupId>
        <artifactId>{i}</artifactId>
        <version>9.0.0-SNAPSHOT</version>

      </project>

    val sbtPath = moduleRoot.resolve("build.sbt")

    val sbt =
      s"""name := "$i"
         |organization := "com.rocketfuel"
         |version := "9.0.0-SNAPSHOT"
       """.stripMargin

    Files.write(pomPath, pom.toString.getBytes)

    Files.write(sbtPath, sbt.getBytes)

    val srcRoot = moduleRoot.resolve("src/main/java")
    Files.createDirectories(srcRoot)

    for (j <- 0 until sourcesPerModule) {
      val javaPath = srcRoot.resolve(s"C$j.java")

      val javaContents = s"public class C$j {}"

      Files.write(javaPath, javaContents.getBytes)
    }
  }

}
