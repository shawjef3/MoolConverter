package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.db.gradle.GradleConvert.loadResource
import com.rocketfuel.build.db.mvn._
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.sdbc.PostgreSql._

object SimpleGradleConvert {

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = Copy.all.vector().toSet
    val fileCopier = FileCopier(copies, moolRoot, destinationRoot)
    fileCopier.copyAll()
  }

  def builds(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {

    val modulePaths = {
      for (ModulePath(id, path) <- ModulePath.list.iterator()) yield
        id -> path
    }.toMap

    val identifiers = {
      for (i <- Identifier.list.iterator()) yield {
        i.bldId -> i
      }
    }.toMap

    val dependencies =
      com.rocketfuel.build.db.mvn.Dependency.list.vector().groupBy(_.sourceId)

    val localBlds = Bld.localBlds.vector()

    var includedBuilds = List[(String, Seq[String])]()
    for (bld <- localBlds) {
      val identifier = identifiers(bld.id)
      println("creating build file for " + identifier)
      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)

      val path = modulePaths(bld.id)
      includedBuilds = (path, bld.path) :: includedBuilds
      val modulePath = destinationRoot.resolve(path)
      val gradle = bld.gradle(identifier, bldDependencies, destinationRoot, modulePath)
      val gradlePath = modulePath.resolve("build.gradle")

      Files.createDirectories(modulePath)
      Files.write(gradlePath, gradle.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    val settingsGradle = moolRoot.resolve("settings.gradle")
    val settings = includedBuilds.sortBy {_._1}.foldLeft("") { (buffer, prjNames) =>
      val comment = if (prjNames._1 == prjNames._2) "" else s" // ${prjNames._2}"
      buffer + s"include ':${prjNames._1}'$comment\n"
    }

    Files.write(settingsGradle,
      (settings + loadResource("settings_end.gradle")).getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)

    //
//    Parents.writeRoot(destinationRoot)
//    Parents.writeCheckStyle(destinationRoot)
//    Parents.`Scala-common`.write(destinationRoot, Set())
//
//    val parentPoms =
//      localBlds.foldLeft(Parents.Poms.Empty) {
//        case (poms, bld) =>
//          val moduleRoot = modulePaths(bld.id)
//          poms.add(bld, moduleRoot)
//      }
//
//    parentPoms.write(destinationRoot)
  }
}
