package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.mvn.{Copy, FileCopier, ModulePath, Parents}
import com.rocketfuel.sdbc.PostgreSql._

/*
TODO: fix extra projects generated from ProjectMappings with no sources
TODO: check if there are copied sources not mapped into build
 */

case class BuildGradleParts(compileOnlyDeps: Set[String] = Set.empty,
                            compileDeps: Set[String] = Set.empty,
                            compileTestDeps: Set[String] = Set.empty,
                            plugins: Set[String] = Set.empty,
                            snippets: Set[String] = Set.empty,
                            useTestNg: Boolean = false)

object GradleConvert extends Logger {
  // not s""": leave interpolation to Groovy/Gradle
  private val protoConfigSnippet =
    """
      |protobuf {
      |  protoc {
      |    path = "${System.env.HOME}/.mooltool/packages/protobuf/bin/protoc"
      |  }
      |}
      |
                    """.stripMargin
  private val protoLib = "  compile files(\"${System.env.HOME}/.mooltool/packages/protobuf/java/target/protobuf-2.5.0.jar\")"

  private def loadResource(path: String): String = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
    try source.mkString
    finally source.close()
  }

  def rootBuildFiles(moolRoot: Path)(implicit connection: Connection) = {
    val prjNameMapping = ProjectMapping.projectNamesMapping().map(_.swap)

    val settingsGradle = moolRoot.resolve("settings.gradle")
    val settings = prjNameMapping.toList.sorted.foldLeft("") { (buffer, prjNames) =>
      val comment = if (prjNames._1 == prjNames._2) "" else s" // ${prjNames._2}"
      buffer + s"include ':${prjNames._1}'$comment\n"
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
      // TODO handle classifiers
      buffer + "  \"" + Library.libReference(lib.path) + "\"" +
        s": '${lib.group_id.get}:${lib.artifact_id.get}:${lib.version.get}',\n"
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
                val dependency = "  compile libraries['" + Library.libReference(lib.path) + "']"
                (build.copy(compileDeps = build.compileDeps + dependency), false)
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
            buildGradleParts.plugins.map(p => s"apply plugin: '${p}'").mkString("\n") +
            buildGradleParts.snippets.mkString("\n") +
            """
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
