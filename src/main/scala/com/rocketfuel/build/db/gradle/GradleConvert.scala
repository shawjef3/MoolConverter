package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.db.mvn.{Copy, FileCopier, ModulePath, Parents}
import com.rocketfuel.sdbc.PostgreSql._

object GradleConvert {

  def normalizeProjectName(prjName: String) : String = {
    // java-com-rocketfuel-grid-externalreport-grid.externalreport => java-com-rocketfuel-grid-externalreport-grid.externalreport
    val prjNameSegments = prjName.split('-').toVector
    // TODO handle path with ','
    if (prjNameSegments.size > 1 && prjNameSegments(prjNameSegments.size - 2) == prjNameSegments(prjNameSegments.size - 1))
      prjNameSegments.dropRight(1).mkString("-")
    else
      prjName
  }

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = GradleCopy.all.vector().filter(!_.destination.contains(',')).map { c =>
      val prjPath = c.destination.split("/").toList
      val fixedDestination = (normalizeProjectName(prjPath.head) :: prjPath.tail).mkString("/")
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
