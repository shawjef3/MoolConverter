package com.rocketfuel.build.db

import java.nio.file._
import com.rocketfuel.build.db.mvn.{Copy, FileCopier, ModulePath, Parents}
import com.rocketfuel.sdbc.PostgreSql._

object Convert {

  val athenaPrefix = "java/com/rocketfuel/modeling/athena".split('/')

  def addBuildRoot(pom: String): String = {
    pom.replace(
      "</properties>",
      """</properties>
        |  <build>
        |    <plugins>
        |      <plugin>
        |        <groupId>me.jeffshaw.scalatest</groupId>
        |        <artifactId>scalatest-maven-plugin</artifactId>
        |        <version>2.0.0-M1</version>
        |        <configuration>
        |          <environmentVariables>
        |            <BUILD_ROOT>${project.parent.parent.parent.parent.basedir}</BUILD_ROOT>
        |          </environmentVariables>
        |        </configuration>
        |      </plugin>
        |    </plugins>
        |  </build>
        |""".stripMargin
    )
  }

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = Copy.all.vector().toSet
    val fileCopier = FileCopier(copies, moolRoot, destinationRoot)
    fileCopier.copyAll()

    //copy testdata
    val testData = moolRoot.resolve("java/com/rocketfuel/modeling/athena/testdata")
    val testDataDestination = destinationRoot.resolve("java/com/rocketfuel/modeling/athena/testdata")
    FileCopier.copyFiles(testData, testDataDestination)
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

    val localBlds = mool.Bld.localBlds.vector()

    val exclusions = mvn.Exclusion.byBldId()

    for (bld <- localBlds) {
      val identifier = identifiers(bld.id)
      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)

      val path = modulePaths(bld.id)
      val modulePath = destinationRoot.resolve(path)
      val pom = bld.pom(identifier, bldDependencies, destinationRoot, modulePath, exclusions)
      val pomPath = modulePath.resolve("pom.xml")

      //This is a hack for athena testdata
      val fixedPom =
        if (bld.path.startsWith(athenaPrefix)) {
          addBuildRoot(pom.toString)
        } else pom.toString

      Files.createDirectories(modulePath)
      Files.write(pomPath, fixedPom.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
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
