package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.build.db.mvn.{Dependency => MvnDependency, _}

class GradleConvert(
  projectRoot: Path,
  modulePaths: Map[Int, String],
  moduleOutputs: Map[String, Int]
) {

  import GradleConvert._

  private def gradleForBld(
    path: String,
    prjBld: Bld,
    dependencies: Vector[MvnDependency],
    moduleRoot: Path,
    exclusions: Map[Int, Set[Exclusion]]
  ): BuildGradleParts = {

    val isTest = prjBld.ruleType.contains("_test")

    val dependencyList: Set[String] = dependencies.foldLeft(List[String]()) { case (depList, dep) =>
      moduleOutputs.get(dep.gradleDefinition).flatMap(modulePaths.get(_)) match {
        case Some(depPath) =>
          val configuration = dep.scope match {
            case "provided" => "compileOnly"
            case "test" => "testCompile"
            case _ => if (isTest) "testCompile" else "compile"
          }
          val projectOutputs = dep.`type` match {
            case Some("test-jar") => List(
              s"project(':${depPath}')",
              s"project(path: ':${depPath}', configuration: 'tests')")
            case _ => List(
              s"project(':${depPath}')")
          }
          depList ++ projectOutputs.map(output => s"  ${configuration} ${output}")
        case _ =>
          depList ++ List(dep.gradleDependency(exclusions)).map { d =>
            if (isTest) d.replace(" compile ", " testCompile ")
            else d
          }
      }
    }.filterNot { testDep =>
      if (testDep.contains("':" + path + "'")) {
        logger.info(s"eliminate test dependency on main source in ${path}")
      }
      testDep.contains("':" + path + "'")
    }.toSet

    val buildGradleParts = BuildGradleParts.valueOf(prjBld, dependencyList)

    buildGradleParts
  }

  def gradle(
    path: String,
    bldWithDeps: Map[Bld, Vector[MvnDependency]],
    moduleRoot: Path,
    exclusions: Map[Int, Map[Int, Set[Exclusion]]]
  ): String = {
    val buildGradleParts =
      bldWithDeps.map { case (bld, deps) => gradleForBld(path, bld, deps, moduleRoot, exclusions.getOrElse(bld.id, Map.empty)) }
      .reduceLeft { (buildParts1, buildParts2) =>
        BuildGradleParts(
          compileDeps = buildParts1.compileDeps ++ buildParts2.compileDeps,
          snippets = buildParts1.snippets ++ buildParts2.snippets,
          plugins = buildParts1.plugins ++ buildParts2.plugins
        )
      }

    val buildGradleText =
      buildGradleParts.plugins.map(p => s"apply ${p}").mkString("\n") + "\n\n" +
        buildGradleParts.snippets.mkString("\n") +
        """
          |
          |dependencies {
          |""".stripMargin +
        buildGradleParts.compileDeps.toSeq.sorted.mkString("\n") +
        "\n}\n"
    buildGradleText
  }

}

object GradleConvert extends Logger {

  def loadResource(path: String): String = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
    try source.mkString
    finally source.close()
  }

}
