package com.rocketfuel.build.db.mvn

import java.nio.file.{Files, Path}

import scala.xml.Elem

object Parents {

  case class File(
    path: String,
    contents: String
  ) {
    def write(root: Path): Unit = {
      val fullPath = root.resolve(path)
      Files.createDirectories(fullPath.getParent)
      Files.write(fullPath, contents.getBytes)
    }

    val module: Elem =
      <module>
        {"parents/" + path.split('/').head}
      </module>
  }

  object File {
    def load(path: String): File = {
      val source = io.Source.fromInputStream(getClass.getResourceAsStream(s"parents/$path"))
      try File(
        path = path,
        contents = source.mkString
      )
      finally source.close()
    }
  }

  val checkstyle = File.load("checkstyle/src/main/resources/com/rocketfuel/poms/checkstyle.xml")

  val checkstylePom = File.load("checkstyle/pom.xml")

  val clojure = File.load("clojure/pom.xml")

  val java = File.load("java/pom.xml")

  val `scala-2.10` = File.load("scala-2.10/pom.xml")

  val `scala-2.11` = File.load("scala-2.11/pom.xml")

  val `scala-2.12` = File.load("scala-2.12/pom.xml")

  val files =
    Set(
      checkstyle,
      checkstylePom,
      clojure,
      java,
      `scala-2.10`,
      `scala-2.11`,
      `scala-2.12`
    )

  def write(path: Path): Unit = {
    for (file <- files)
      file.write(path)
  }

  val modules: Set[Elem] =
    files.map(_.module)

}
