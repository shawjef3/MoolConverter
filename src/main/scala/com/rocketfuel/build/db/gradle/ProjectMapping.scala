package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class ProjectMapping(prj_path: String, bld_id: Int, bld_path: String)

object ProjectMapping extends Deployable with Logger {
  val list = Select[ProjectMapping]("SELECT prj_path, bld_id, bld_path FROM gradle.project_mapping")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "project_mapping.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.project_mapping CASCADE")

  def normalizeProjectName(prjName: String, bldPaths: Vector[String] = Vector.empty): String = {
    val normalized = normalizeProjectNameImpl(prjName, bldPaths)
    if (normalized.length > 50) normalized.substring(0, 50) else normalized
  }

  private def normalizeProjectNameImpl(prjName: String, bldPaths: Vector[String] = Vector.empty): String = {
    def longestCommonPrefix(list: String*) = list.foldLeft("")((_, _) =>
      (list.min.view, list.max.view).zipped.takeWhile(v => v._1 == v._2).unzip._1.mkString)

    val prjNames = prjName.split(',').toVector
    if (prjNames.size < 2)
      normalizeSingleProjectName(prjName)
    else {
      val normalPrjNames = prjNames.map { normalizeSingleProjectName(_) }
      val prefix = longestCommonPrefix(normalPrjNames: _*)
      val shorterPrjName = prefix + normalPrjNames.map { _.substring(prefix.length)}.mkString(",")

      if (shorterPrjName.length > 100) {
        logger.warn(s"project name too long $shorterPrjName")
        val fromBldPaths = bldPaths.mkString(",")
        val normalFromBldPaths = normalizeProjectName(fromBldPaths)
        if (!normalFromBldPaths.isEmpty && normalFromBldPaths.length < shorterPrjName.length)
          "lib-" + normalFromBldPaths else shorterPrjName
      }
      else
        shorterPrjName
    }
  }

  private def normalizeSingleProjectName(prjName: String): String = {
    val prjNameSegments = prjName
        .replaceAll("-analyze_coefficients", "")
        .replaceAll("grid.epiphany.jobs.", "")
        .replaceAll("grid.epiphany.", "")
        .replaceAll("perfdiagnozer_", "")
        .replaceAll("-retargeting", "-rt")
        .replaceAll("java-com-rocketfuel-modeling-athena-pipelines-conversion", "j-c-r-m-a-p-c")
        .replaceAll("java-com-rocketfuel-modeling-athena-pipelines", "j-c-r-m-a-p")
        .replaceAll("java-com-rocketfuel-modeling-athena", "j-c-r-m-a")
        .replaceAll("java-com-rocketfuel-common-message-protobuf", "protobuf")
      .replaceAll("java-com-rocketfuel", "j-c-r").split('-').toVector
    val shortPrjName = prjNameSegments.dropRight(1).mkString("-")

    if (shortPrjName.endsWith(prjNameSegments(prjNameSegments.size - 1).replace('.', '-')) ||
      prjNameSegments(prjNameSegments.size - 1).matches("[a-zA-Z_]+_pipeline"))
      shortPrjName else prjNameSegments.mkString("-")
  }

  def projectNames()(implicit connection: Connection): collection.SortedSet[String] = {
    val prjBldMap: Map[String, Vector[String]] = list.vector().filter { pm => !pm.bld_path.startsWith("java-mvn")
    }.foldLeft(Map.empty[String, Vector[String]]) { (map, pm) =>
      map + (pm.prj_path -> (map.getOrElse(pm.prj_path, Vector.empty) :+ pm.bld_path))
    }
    val prjNames = prjBldMap.map { case (k, v) => normalizeProjectName(k, v) }.toSeq
    collection.SortedSet(prjNames: _*)
  }
}
