package com.rocketfuel.build.db

import java.nio.file._
import com.rocketfuel.build.db.mvn.{Copy, FileCopier, ModulePath, Parents}
import com.rocketfuel.sdbc.PostgreSql._

object Convert {

  //This is a hack for athena testdata
  val usesTestData = Set(
    "java/com/rocketfuel/modeling/athena/pipelines/featureDist/FeatureDistTest",
    "java/com/rocketfuel/modeling/athena/core/utils/UtilsTest",
    "java/com/rocketfuel/modeling/athena/core/utils/UtilsSparkTest",
    "java/com/rocketfuel/modeling/athena/core/utils/Utils",
    "java/com/rocketfuel/modeling/athena/core/modules/ml/MLModulesTest",
    "java/com/rocketfuel/modeling/athena/core/modules/LogisticRegressionOWLQNModuleTest",
    "java/com/rocketfuel/modeling/athena/core/modules/EncodingModulesTest",
    "java/com/rocketfuel/modeling/athena/core/modules/EncodingModulesTest",
    "java/com/rocketfuel/modeling/athena/core/modules/BacktestAUCCalculationModuleTest",
    "java/com/rocketfuel/modeling/athena/core/ml/models/ParamTest",
    "java/com/rocketfuel/modeling/athena/core/common/TestSetup",
    "java/com/rocketfuel/modeling/athena/core/common/JsonConverterTest",
    "java/com/rocketfuel/modeling/athena/core/common/FeatureGroupingFunctionsTest",
    "java/com/rocketfuel/modeling/athena/core/common/ConfLiftTest",
    "java/com/rocketfuel/modeling/athena/core/common/CommonTest",
    "java/com/rocketfuel/modeling/athena/core/common/BacktestUtilsTest"
  ).map(_.split("/").toSeq)

  def addBuildRoot(pom: String): String = {
    pom.replace(
      "</properties>",
      """</properties>
        |  <build>
        |    <plugins>
        |      <plugin>
        |        <groupId>me.jeffshaw.scalatest</groupId>
        |        <artifactId>scalatest-maven-plugin</artifactId>
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

    for (bld <- localBlds) {
      val identifier = identifiers(bld.id)
      val bldDependencies = dependencies.getOrElse(bld.id, Vector.empty)

      val path = modulePaths(bld.id)
      val modulePath = destinationRoot.resolve(path)
      val pom = bld.pom(identifier, bldDependencies, destinationRoot, modulePath)
      val pomPath = modulePath.resolve("pom.xml")

      val fixedPom =
        if (usesTestData.contains(bld.path)) {
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
