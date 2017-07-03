package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.db.mvn.{Copy, FileCopier, ModulePath, Parents}
import com.rocketfuel.sdbc.PostgreSql._

/*
TODO: fix extra projects generated from ProjectMappings with no sources
TODO: check if there are copied sources not mapped into build
 */

object GradleConvert {
  private def loadResource(path: String): String = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
    try source.mkString
    finally source.close()
  }

  def rootBuildFiles(moolRoot: Path)(implicit connection: Connection) = {
    val prjMappings = ProjectMapping.projectNames()

    val settingsGradle = moolRoot.resolve("settings.gradle")
    val settings = prjMappings.foldLeft("") { (buffer, prjName) =>
      buffer + s"include ':$prjName'\n"
    }

    Files.write(settingsGradle,
      (settings + loadResource("settings_end.gradle")).getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)

    val librariesGradle = moolRoot.resolve("gradle/libraries.gradle")
    val libraries = Library.list.vector().filter { lib =>
      lib.path.startsWith("java.mvn.") && lib.group_id.isDefined && lib.artifact_id.isDefined && lib.version.isDefined
    }.sorted.foldLeft("ext.libraries = [\n") { (buffer, lib) =>
      // TODO handle scala version
      // TODO handle classifiers
      buffer + s"  ${Library.libReference(lib.path)}: '${lib.group_id.get}:${lib.artifact_id.get}:${lib.version.get}',\n"
    }
    Files.write(librariesGradle,
      (libraries + "]\n").getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)
  }


  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val prjNameMapping = ProjectMapping.projectNamesMapping()
    val copies = GradleCopy.all.vector().map { c =>
      val prjPath = c.destination.split("/").toList
      val fixedDestination = (prjNameMapping(prjPath.head) :: prjPath.tail).mkString("/")
      c.copy(destination = fixedDestination)
    }.toSet
    val fileCopier = FileCopier(copies, moolRoot, destinationRoot)
    fileCopier.copyAll()
  }

  def builds(destinationRoot: Path)(implicit connection: Connection): Unit = {
//
//    val modulePaths = {
//      for (ModulePath(id, path) <- ModulePath.list.iterator()) yield
//        id -> path
//    }.toMap
//
//    val identifiers = {
//      for (i <- mvn.Identifier.list.iterator()) yield {
//        i.bldId -> i
//      }
//    }.toMap
//
//    val dependencies =
//      mvn.Dependency.list.vector().groupBy(_.sourceId)
//
//    val localBlds = mool.Bld.localBlds.vector()
//
//    for (bld <- localBlds) {
//      val identifier = identifiers(bld.id)
//      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)
//
//      val path = modulePaths(bld.id)
//      val modulePath = destinationRoot.resolve(path)
//      val pom = bld.pom(identifier, bldDependencies, destinationRoot, modulePath)
//      val pomPath = modulePath.resolve("pom.xml")
//
//      Files.createDirectories(modulePath)
//      Files.write(pomPath, pom.toString.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
//    }
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
