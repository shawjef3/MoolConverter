package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.gradle.GradleConvert.loadResource
import com.rocketfuel.build.db.mvn._
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.sdbc.PostgreSql._

object SimpleGradleConvert extends Logger {

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = Copy.all.vector().toSet
    Copy.copy(copies, moolRoot, destinationRoot)
  }

  def builds(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val projectsRoot = destinationRoot.resolve("projects")

    val modulePaths = {
      for (ModulePath(id, path) <- ModulePath.list.iterator()) yield
        id -> path.replaceAll("/", "-")
    }.toMap

    val identifiers = {
      for (i <- Identifier.list.iterator()) yield {
        i.bldId -> i
      }
    }.toMap

    val dependencies =
      com.rocketfuel.build.db.mvn.Dependency.list.vector().groupBy(_.sourceId)

    val localBlds = Bld.locals.vector()

    var includedBuilds = List[(String, Seq[String])]()
    val moduleOutputs = localBlds.foldLeft(Map.empty[String, Int]) { case (moduleOuts, bld) =>
      val identifier = identifiers(bld.id)
      val output = s"${identifier.groupId}:${identifier.artifactId}:${identifier.version}"
      if (output.contains("Duplex")) {
        logger.info(s"${output} produced by ${bld}")
      }
      moduleOuts + (output -> bld.id)
    }
    for (bld <- localBlds) {
      val identifier = identifiers(bld.id)
      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)

      val path = modulePaths(bld.id)
      includedBuilds = (path, bld.path) :: includedBuilds
      val modulePath = projectsRoot.resolve(path.replaceAll("-", "/"))
      val gradle = GradleConvert.gradle(identifier, bld, bldDependencies, projectsRoot,
        modulePath, modulePaths, moduleOutputs)
      val gradlePath = modulePath.resolve("build.gradle")

      Files.createDirectories(modulePath)
      Files.write(gradlePath, gradle.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    val settingsGradle = destinationRoot.resolve("settings.gradle")
    val settings = includedBuilds.sortBy {_._1}.foldLeft("") { (buffer, prjNames) =>
      val comment = if (prjNames._1 == prjNames._2) "" else s" // ${prjNames._2}"
      buffer + s"include ':${prjNames._1}'$comment\n"
    }

    Files.write(settingsGradle,
      (settings + loadResource("settings_end.gradle")).getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)

  }
}
