package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._
import java.nio.file.{Files, Path, StandardCopyOption, StandardOpenOption}

case class Copy(
  source: String,
  packagePath: String,
  destination: String
)

object Copy extends Deployable {
  val all =
    Select[Copy]("SELECT source, package_path AS packagePath, destination FROM mvn.copies")

  val deployQuery = Ignore.readClassResource(classOf[Identifier], "copies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.copies CASCADE")

  /**
    * Run multiple copies.
    *
    * Only copies a file once. If multiple copies of the same file are
    * required, then links are created to the first copy.
    */
  def copy(
    copies: Set[Copy],
    sourceRoot: Path,
    destinationRoot: Path
  ): Unit = {
    val protoCopies =
      copies.filter(_.source.endsWith(".proto"))

    def copy(source: Path, destination: Path): Unit = {
      Files.createDirectories(destination.getParent)
      if (source.getFileName.endsWith(".proto")) {
        copyProto(source, destination)
      } else {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
      }
    }

    /**
      * Most files can be copied verbatim. However, protoc files have imports that use the literal path
      * to dependent protoc files. These paths change with the conversion to maven projects.
      */
    def copyProto(source: Path, destination: Path): Unit = {
      val sourceContents = new String(Files.readAllBytes(source))

      val destinationContents =
        protoCopies.foldLeft(sourceContents) {
          case (accum, Copy(importSource, importPackagePath, _)) =>
            /*
            The protoc maven plugin puts the protoc files in the proto_path for us,
            so we just have to give the path relative to the source root.
             */
            accum.replace(importSource, importPackagePath)
        }

      Files.write(destination, destinationContents.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    copies.foldLeft(Map.empty[String, Path]) {
      case (previousCopies, Copy(source, _, destination)) =>
        val sourcePath = sourceRoot.resolve(source)
        val destinationPath = destinationRoot.resolve(destination)

        previousCopies.get(source) match {
          case None =>
            copy(sourcePath, destinationPath)
            previousCopies + (source -> destinationPath)

          case Some(existingDestination) =>
            val target = destinationPath.getParent.relativize(existingDestination)

            Files.deleteIfExists(destinationPath)

            Files.createSymbolicLink(destinationPath, target)
            previousCopies
        }
    }
  }

}
