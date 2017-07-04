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
                            useTestNg: Boolean = false)

object GradleConvert extends Logger {
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
          val buildGradleText =
            """dependencies {
              |""".stripMargin
          val buildGradleParts = deps.sortBy(_.path).foldLeft((BuildGradleParts(), true)) {
            case ((build, isFirst), lib) =>
              if (!Files.isDirectory(prjBuildGradle.getParent) && isFirst) {
                logger.warn(s"Project dir ${prjBuildGradle.getParent} does not exist")
              }

              // TODO project cross-deps
              if (lib.isMavenDep) {
                val dependency = Library.libReference(lib.path)
                // s"  compile libraries['${dependency}']\n"
                (build.copy(compileDeps = build.compileDeps + dependency), false)
              } else if (bldIdToPrjPath(lib.id) == prjPath) {
                // BLD from our project. check for proto, scala and more to adjust project
                logger.warn(s"Customize ${prjPath} for ${lib}")
                (build, false)
              } else {
                hasNonMavenDep = true
                (build, false)
              }
          }._1

          if (Files.isDirectory(prjBuildGradle.getParent)) {
            Files.write(prjBuildGradle,
              (buildGradleText + buildGradleParts.compileDeps.map { dep =>
                s"  compile libraries['${dep}']"}.mkString("\n") + "}\n").getBytes,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.CREATE)
          }
    }
  }
}
