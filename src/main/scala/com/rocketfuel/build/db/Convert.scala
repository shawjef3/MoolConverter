package com.rocketfuel.build.db

import java.nio.file._
import com.rocketfuel.build.db.mvn.{Copy, ModulePath, Parents}
import com.rocketfuel.sdbc.PostgreSql._

object Convert {

  val athenaPrefix = "java/com/rocketfuel/modeling/athena".split('/')

  def copyFiles(source: Path, destination: Path): Unit = {
    Files.createDirectories(destination)
    Files.walk(source).filter(Files.isRegularFile(_)).forEach(
      path => {
        val relativePath = source.relativize(path)
        val pathDestination = destination.resolve(relativePath)
        Files.createDirectories(pathDestination.getParent)
        Files.copy(path, pathDestination, StandardCopyOption.REPLACE_EXISTING)
      }
    )
  }

  /**
    * Athena tests often require files be in a specific location relative to the environment variable, BUILD_ROOT.
    * To accommodate this, create a link from java to /testdata/java, and for tests set
    * an environment variable BUILD_ROOT to ${basedir}.
    */
  def linkTestData(destinationRoot: Path)(implicit connection: Connection): Unit = {
    val modulePaths = ModulePath.byId()
    for (bld <- mool.Bld.athenaTests.iterator()) {
      val moduleRelativePath = modulePaths(bld.id).path
      val modulePath = destinationRoot.resolve(moduleRelativePath)
      val link = modulePath.resolve("java")
      val testdata = destinationRoot.resolve("testdata/java")
      val relativeTarget = link.getParent.relativize(testdata)

      Files.createDirectories(modulePath)
      if (Files.exists(link))
        Files.delete(link)

      Files.createSymbolicLink(link, relativeTarget)
    }
  }

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = Copy.all.vector().toSet
    Copy.copy(copies, moolRoot, destinationRoot)

    //copy testdata
    val testData = moolRoot.resolve("java/com/rocketfuel/modeling/athena/testdata")
    val testDataDestination = destinationRoot.resolve("testdata/java/com/rocketfuel/modeling/athena/testdata")
    copyFiles(testData, testDataDestination)

    linkTestData(destinationRoot)
  }

  def poms(destinationRoot: Path)(implicit connection: Connection): Unit = {

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

    val localBlds = mool.Bld.locals.vector()

    val exclusions = mvn.Exclusion.byBldIdAndDependencyId()

    for (bld <- localBlds) {
      val identifier = identifiers(bld.id)
      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)

      val path = modulePaths(bld.id)
      val modulePath = destinationRoot.resolve(path)
      val pom = bld.pom(identifier, bldDependencies, destinationRoot, modulePath, exclusions)
      val pomPath = modulePath.resolve("pom.xml")

      Files.createDirectories(modulePath)
      Files.write(pomPath, pom.toString.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    Parents.writeRoot(destinationRoot)
    Parents.writeCheckStyle(destinationRoot)
    Parents.`Scala-common`.write(destinationRoot, Set())

    val parentPoms =
      localBlds.foldLeft(Parents.Poms.Empty) {
        case (poms, bld) =>
          val moduleRoot = modulePaths(bld.id)
          poms.add(bld, moduleRoot)
      }

    parentPoms.write(destinationRoot)
  }

}
