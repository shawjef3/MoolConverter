package com.rocketfuel.build.db.mvn

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import com.rocketfuel.build.db.mool.Bld

object Parents {

  def loadResource(path: String): String = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
    try source.mkString
    finally source.close()
  }

  val root = loadResource("root/pom.xml")

  def writeRoot(projectRoot: Path): Unit = {
    val pomPath = projectRoot.resolve("pom.xml")
    Files.write(pomPath, root.getBytes)
  }

  val checkstyleXml = loadResource("checkstyle/checkstyle.xml")

  def writeCheckstyleXml(projectRoot: Path): Unit = {
    val checkStylePath = projectRoot.resolve("checkstyle/src/resources/com/rocketfuel/checkstyle.xml")
    Files.createDirectories(checkStylePath.getParent)
    Files.write(checkStylePath, checkstyleXml.getBytes)
  }

  def writeCheckStyle(projectRoot: Path): Unit = {
    writeCheckstyleXml(projectRoot)
    checkstyle.write(projectRoot, Set())
  }

  case class Pom(
    path: Path,
    contents: String
  ) {

    val pomParent = path.getParent

    def createPom(projectRoot: Path, modulePaths: Set[String]): String = {
      val pomProjectRoot = projectRoot.resolve(pomParent)

      val modules =
        for (modulePath <- modulePaths) yield {
          val modulePathPath = projectRoot.resolve(modulePath)
          val moduleRelativePath = pomProjectRoot.relativize(modulePathPath)
          s"<module>$moduleRelativePath</module>"
        }

      contents.replace(Pom.ModulesString, modules.mkString("\n"))
    }

    def write(projectRoot: Path, modulePaths: Set[String]): Unit = {
      val pom = createPom(projectRoot, modulePaths)
      val pomPath = projectRoot.resolve(path)

      Files.createDirectories(pomPath.getParent)
      Files.write(pomPath, pom.getBytes, StandardOpenOption.TRUNCATE_EXISTING)
    }

    val artifactId =
      path.getParent.getFileName

    def parentXml(projectRoot: Path, moduleRoot: Path) = {
      val pathToParent = moduleRoot.relativize(projectRoot.resolve(pomParent))
      <parent>
        <groupId>com.rocketfuel.parents</groupId>
        <artifactId>{artifactId}</artifactId>
        <relativePath>{pathToParent}</relativePath>
        <version>M1</version>
      </parent>
    }

  }

  object Pom {
    val parents = Paths.get("parents")

    def load(path: String): Pom = {
      val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
      try Pom(
        path = Paths.get(path),
        contents = source.mkString
      )
      finally source.close()
    }

    val ModulesString = "$MODULES$"
  }

  val checkstyle = Pom.load("checkstyle/pom.xml")

  val Clojure = Pom.load("parents/clojure/pom.xml")

  val Java = Pom.load("parents/java/pom.xml")

  val Protobuf = Pom.load("parents/protobuf/pom.xml")

  val `Scala-common` = Pom.load("parents/scala-common/pom.xml")

  val `Scala-2.10` = Pom.load("parents/scala-2.10/pom.xml")

  val `Scala-2.11` = Pom.load("parents/scala-2.11/pom.xml")

  val `Scala-2.12` = Pom.load("parents/scala-2.12/pom.xml")

  val Thrift = Pom.load("parents/thrift/pom.xml")

  case class Poms(modules: Map[Pom, Set[String]]) {
    def add(
      bld: Bld,
      modulePath: String
    ): Poms = {
      val key = parent(bld)
      copy(modules = modules + (key -> (modules.getOrElse(key, Set()) + modulePath)))
    }

    def write(projectRoot: Path): Unit = {
      for ((pom, pomModules) <- modules) {
        pom.write(projectRoot, pomModules)
      }
    }
  }

  object Poms {
    val Empty =
      Poms(Map.empty)
  }

  def parent(bld: Bld): Pom = {
    (bld.ruleType, bld.scalaVersion) match {
      case ("scala_test" |
            "scala_bin" |
            "scala_lib", Some(scalaVersion)) =>
        scalaVersion match {
          case "2.10" =>
            `Scala-2.10`
          case "2.11" =>
            `Scala-2.10`
          case "2.12" =>
            `Scala-2.12`
        }

      case ("clojure_lib" |
            "clojure_bin", None) =>
        Clojure

      case ("java_test" |
            "java_bin" |
            "java_lib", None) =>
        Java

      case ("java_proto_lib", None) =>
        Protobuf

      case ("java_thrift_lib", None) =>
        Thrift

      case (_, None) =>
        //TODO: maybe a better default
        Java
    }
  }

}
