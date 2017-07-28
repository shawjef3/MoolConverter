package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.build.db.mvn.{Dependency => MvnDependency}
import com.rocketfuel.build.db.mvn._
import com.rocketfuel.sdbc.PostgreSql._

/*
TODO: fix extra projects generated from ProjectMappings with no sources
TODO: check if there are copied sources not mapped into build
 */

case class BuildGradleParts(compileOnlyDeps: Set[String] = Set.empty,
                            compileDeps: Set[String] = Set.empty,
                            compileTestDeps: Set[String] = Set.empty,
                            plugins: Set[String] = Set("plugin: 'java'"),
                            snippets: Set[String] = Set.empty)

object GradleConvert extends Logger {
  // not s""": leave interpolation to Groovy/Gradle
  private val protoConfigSnippet =
    """
      |protobuf {
      |  protoc {
      |    path = "${System.env.HOME}/.mooltool/packages/protobuf/bin/protoc"
      |  }
      |}
      |task sourcesJar(type: Jar, dependsOn: classes) {
      |    classifier = 'sources'
      |    from sourceSets.main.allSource
      |    from sourceSets.main.proto
      |    from "${protobuf.generatedFilesBaseDir}/main/java"
      |}
      |idea {
      |    module {
      |        sourceDirs += file("${protobuf.generatedFilesBaseDir}/main/java");
      |    }
      |}
      |""".stripMargin
  private val thriftConfigSnippet =
    """
      |compileThrift {
      |  thriftExecutable "${System.env.HOME}/.mooltool/packages/thrift-0.9.1/bin/thrift"
      |}
      |""".stripMargin
  private val testNGConfigSnippetWithGroupsPre =
    """
      |test {
      |  useTestNG() {
      |    includeGroups """.stripMargin
  private val testNGConfigSnippetWithGroupsPost =
    """
      |  }
      |}
      |""".stripMargin
  def testNGConfig(testGroups: Option[String]): String = {
    testNGConfigSnippetWithGroupsPre +
      testGroups.getOrElse("unit").split(",").map { "'" + _ + "'"}.mkString(", ") +
      testNGConfigSnippetWithGroupsPost
  }
  private val shadowJarSnippet =
    """shadowJar {
      |  manifest {
      |    attributes 'Main-Class': '__MAIN_CLASS__'
      |  }
      |}
      |""".stripMargin
  def shadowJarConfig(mainClass: Option[String]): Option[String] =
    mainClass.map(mClz => shadowJarSnippet.replace("__MAIN_CLASS__", mClz))

  private val scala210Libs = List("  compile 'org.scala-lang:scala-library:2.10.4'",
    "  compile 'org.scala-lang:scala-actors:2.10.4'"
  )
  private val scala211Libs = List("  compile 'org.scala-lang:scala-library:2.11.8'")
  // use 1.4.2 with scalatest 3, now stick to 1.1
  private val scalatestLibs = "  testRuntime 'org.pegdown:pegdown:1.1.0'"
  private val scalatestSnippet =
    """
      |test {
      |    maxParallelForks = 1
      |}
    """.stripMargin
  private val scala210Tasks = "rootProject.tasks.build210.dependsOn tasks.build\n"
  private val scala211Tasks = "rootProject.tasks.build211.dependsOn tasks.build\n"

  private val protoLib = "  compile files(\"${System.env.HOME}/.mooltool/packages/protobuf/java/target/protobuf-2.5.0.jar\")"
  private val thriftLib = "  compile 'org.apache.thrift:libthrift:0.9.1'"

  def loadResource(path: String): String = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
    try source.mkString
    finally source.close()
  }

  def rootBuildFiles(moolRoot: Path)(implicit connection: Connection) = {
    val prjNameMapping = ProjectMapping.projectNamesMapping().map(_.swap)

    val settingsGradle = moolRoot.resolve("settings.gradle")
    val settings = prjNameMapping.toList.sorted.foldLeft("") { (buffer, prjNames) =>
      val prjName = prjNames._1.replaceAll("/", "-")
      val comment = if (prjNames._1 == prjNames._2) "" else s" // ${prjNames._2}"
      buffer + s"include ':${prjName}'$comment\n"
    }

    Files.write(settingsGradle,
      (settings + loadResource("settings_end.gradle")).getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)

    val librariesGradle = moolRoot.resolve("gradle/libraries.gradle")
    val libraries = Library.list.vector().filter { lib =>
      lib.isMavenDep
    }.sorted.foldLeft("ext.libraries = [\n") { (buffer, lib) =>
      // TODO handle scala version
      val classifier = ":" + lib.classifier.getOrElse("")
      buffer + "  \"" + Library.libReference(lib.path) + "\"" +
        s": '${lib.group_id.get}:${lib.artifact_id.get}:${lib.version.get}${classifier}',\n"
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
      val fixedDestination2 = if (fixedDestination.endsWith("AdScoringInfoPerseus.java"))
                                 fixedDestination.replace("src/main/java/", "src/main/scala/")
                              else
                                 fixedDestination
      c.copy(destination = fixedDestination2)
    }.toSet
    throw new RuntimeException("FileCopier needs to be updated to Convert.copyFiles or Copy.copy")
//    val fileCopier = FileCopier(copies, moolRoot, destinationRoot)
//    fileCopier.copyAll()
  }


  def sourceCompatibility(javaVersion: Option[String]): String =
    "sourceCompatibility = " + javaVersion.getOrElse("1.7")

  private def gradleForBld(prjBld: Bld, dependencies: Vector[MvnDependency], projectRoot: Path,
             moduleRoot: Path, modulePaths: Map[Int, String], moduleOutputs: Map[String, Int]) = {
    lazy val dependencyList = dependencies.foldLeft(List[String]()) { case (depList, dep) =>
      moduleOutputs.get(dep.gradleDefinition).flatMap(modulePaths.get(_)) match {
        case Some(depPath) =>
          val configuration = dep.scope match {
            case "provided" => "compileOnly"
            case "test" => "testCompile"
            case _ => "compile"
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
          depList ++ List(dep.gradleDependency)
      }
    }.toSet

    val buildGradleParts = {
      prjBld.ruleType match {
        case "java_proto_lib" =>
          BuildGradleParts(compileDeps = Set(protoLib) ++ dependencyList,
            plugins = Set("plugin: 'java'", "plugin: 'com.google.protobuf'"),
            snippets = Set(protoConfigSnippet))
        case "java_lib" | "file_coll" =>
          BuildGradleParts(compileDeps = dependencyList,
            plugins = Set("plugin: 'java'"),
            snippets = Set(sourceCompatibility(prjBld.javaVersion)))
        case "java_bin" =>
          BuildGradleParts(compileDeps = dependencyList,
            plugins = Set("plugin: 'java'", "plugin: 'com.github.johnrengelman.shadow'"),
            snippets = shadowJarConfig(prjBld.mainClass).toSet + sourceCompatibility(prjBld.javaVersion))
        case "java_test" =>
          // 'from: "${' will be interpolated by Gradle
          BuildGradleParts(plugins = Set("plugin: 'java'", "from: \"${rootProject.projectDir}/gradle/tests.gradle\""),
            snippets = Set(testNGConfig(prjBld.testGroups)) + sourceCompatibility(prjBld.javaVersion),
            compileDeps = dependencyList)
        case "java_thrift_lib" =>
          BuildGradleParts(plugins = Set("plugin: 'java'", "plugin: 'org.jruyi.thrift'"),
            snippets = Set(thriftConfigSnippet) + sourceCompatibility(prjBld.javaVersion),
            compileDeps = Set(thriftLib) ++ dependencyList)
        case "scala_lib" =>
          val compileDeps = prjBld.scalaVersion match {
            case Some("2.10") => scala210Libs.toSet ++ dependencyList
            case Some("2.11") => scala211Libs.toSet ++ dependencyList
            case Some("2.12") => dependencyList // TODO should have 2.12 libs
            case _ =>
              logger.warn(s"scala_lib with unknown version ${prjBld}")
              dependencyList
          }
          BuildGradleParts(compileDeps = compileDeps,
            plugins = Set("plugin: 'scala'"),
            snippets = Set(sourceCompatibility(prjBld.javaVersion)))
        case "scala_test" =>
          val compileDeps = prjBld.scalaVersion match {
            case Some("2.10") => scala210Libs.toSet ++ dependencyList + scalatestLibs
            case Some("2.11") => scala211Libs.toSet ++ dependencyList + scalatestLibs
            case Some("2.12") => dependencyList + scalatestLibs // TODO should have 2.12 libs
            case _ =>
              logger.warn(s"scala_test with unknown version ${prjBld}")
              dependencyList.toSet + scalatestLibs
          }
          BuildGradleParts(compileDeps = compileDeps,
            plugins = Set("plugin: 'scala'", "plugin: 'com.github.maiflai.scalatest'"),
            snippets = Set(sourceCompatibility(prjBld.javaVersion), scalatestSnippet))
        case "scala_bin" =>
          val compileDeps = prjBld.scalaVersion match {
            case Some("2.10") => scala210Libs.toSet ++ dependencyList
            case Some("2.11") => scala211Libs.toSet ++ dependencyList
            case Some("2.12") => dependencyList // TODO should have 2.12 libs
            case _ =>
              logger.warn(s"scala_lib with unknown version ${prjBld}")
              dependencyList
          }
          BuildGradleParts(compileDeps = compileDeps,
            plugins = Set("plugin: 'scala'", "plugin: 'com.github.johnrengelman.shadow'"),
            snippets = shadowJarConfig(prjBld.mainClass).toSet + sourceCompatibility(prjBld.javaVersion))
        case _ =>
          BuildGradleParts(plugins = Set("plugin: 'base'"))
      }
    }
    buildGradleParts
  }

  def gradle(path: String, prjBld: Bld, extraBlds: Map[Bld, Vector[MvnDependency]],
             dependencies: Vector[MvnDependency], projectRoot: Path,
             moduleRoot: Path, modulePaths: Map[Int, String], moduleOutputs: Map[String, Int]) = {
    if (!extraBlds.isEmpty) {
      logger.info(s"prj ${moduleRoot} should merge with ${extraBlds}")
    }
    val buildGradleParts = extraBlds.foldLeft(gradleForBld(prjBld, dependencies, projectRoot, moduleRoot, modulePaths, moduleOutputs))
      { (buildParts, extraBld) =>
        val extraBuildParts = gradleForBld(extraBld._1, extraBld._2, projectRoot, moduleRoot, modulePaths, moduleOutputs)
        // TODO merge those two
        val updatedExtraDeps = extraBuildParts.compileDeps.filterNot { testDep =>
          if (testDep.contains("':" + path + "'")) {
            logger.info(s"eliminate test dependency on main source in ${path}")
          }
          buildParts.compileDeps.contains(testDep) || testDep.contains("':" + path + "'")
        }.map {testDep =>
            testDep.replace("  compile ", "  testCompile ")
        }
        BuildGradleParts(
          compileDeps = buildParts.compileDeps ++ updatedExtraDeps,
          snippets = buildParts.snippets ++ extraBuildParts.snippets,
          plugins = buildParts.plugins ++ extraBuildParts.plugins
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

  def builds(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val prjNameMapping = ProjectMapping.projectNamesMapping()
    val bldIdToPrjPath = ProjectMapping.list.vector().foldLeft(Map.empty[Int, String]) { (map, pm) =>
      map + (pm.bld_id -> pm.prj_path)
    }

    Dependency.list.vector().groupBy {_.prj_path}.filter {
      case (prjPath, deps) =>
        if (!prjNameMapping.contains(prjPath)) {
          if (!deps.isEmpty)
            logger.warn(s"Cannot find project for ${prjPath}")
          false
        } else {
          true
        }
    }.foreach {
        case (prjPath, deps) =>
          val prjBuildGradle = moolRoot.resolve("projects/" + prjNameMapping(prjPath) + "/build.gradle")

          var hasNonMavenDep = false
          val buildGradleParts = deps.sortBy(_.path).foldLeft((BuildGradleParts(), true)) {
            case ((build, isFirst), lib) =>
              // TODO project cross-deps
              if (lib.isMavenDep) {
                val addedDependency = (prjNameMapping(prjPath), Library.libReference(lib.path))

                if (addedDependency !=
                  ("j-c-r-camus,grid-datascrub-grid.scrub", "com.rocketfuel.grid.thirdparty.hive.HiveExec")) {
                  val dependency = "  compile libraries['" + addedDependency._2 + "']"
                  (build.copy(compileDeps = build.compileDeps + dependency), false)
                } else {
                  logger.info(s"Ignored dependency ${addedDependency}")
                  (build, false)
                }
              } else {
                val depPrjPath = bldIdToPrjPath(lib.id)
                if (depPrjPath == prjPath) {
                  // BLD from our project. check for proto, scala and more to adjust project
                  logger.trace(s"Customize ${prjPath} for ${lib}")
                  lib.rule_type match {
                    case r if r == "java_proto_lib" =>
                      (build.copy(compileDeps = build.compileDeps + protoLib,
                        plugins = build.plugins + "com.google.protobuf",
                        snippets = build.snippets + protoConfigSnippet), false)
                    case r if r == "java_test" =>
                      (build.copy(snippets = build.snippets + testNGConfig(None)), false)
                    case r if r == "java_thrift_lib" =>
                      (build.copy(plugins = build.plugins + "org.jruyi.thrift",
                        snippets = build.snippets + thriftConfigSnippet), false)
                    case r if r == "scala_lib" =>
                      (build.copy(
                        compileDeps = build.compileDeps ++ (if (lib.scala_version.contains("2.10")) scala210Libs else scala211Libs),
                        plugins = build.plugins ++ Set("scala", "com.adtran.scala-multiversion-plugin"),
                        snippets = build.snippets + (if (lib.scala_version.contains("2.10")) scala210Tasks else scala211Tasks)), false)
                    case _ =>
                      (build, false)
                  }
                } else {
                  hasNonMavenDep = true
                  val newDeps = prjNameMapping.get(depPrjPath) match {
                    case Some(dependency) =>
                      val dependencyStr = "  compile project(':" + dependency + "')"
                      logger.trace(s"Add dependency on ${depPrjPath} to ${prjPath}")
                      build.compileDeps + dependencyStr
                    case _ =>
                      logger.warn(s"Cannot add dependency on ${depPrjPath} to ${prjPath}")
                      build.compileDeps
                  }
                  (build.copy(compileDeps = newDeps), false)
                }
              }
          }._1

          val buildGradleText =
            buildGradleParts.plugins.map(p => s"apply plugin: '${p}'").mkString("\n") + "\n\n" +
            buildGradleParts.snippets.mkString("\n") +
            """
              |
              |configurations.compile.transitive = false
              |
              |dependencies {
              |""".stripMargin +
            buildGradleParts.compileDeps.toSeq.sorted.mkString("\n") +
            "\n}\n"


          Files.createDirectories(prjBuildGradle.getParent)
          Files.write(prjBuildGradle,
            buildGradleText.getBytes,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE)
    }
  }
}
