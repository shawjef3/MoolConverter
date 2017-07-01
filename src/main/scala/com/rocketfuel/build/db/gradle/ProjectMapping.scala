package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class ProjectMapping(prj_path: String, bld_id: Int, bld_path: String)

object ProjectMapping extends Deployable {
  val list = Select[ProjectMapping]("SELECT prj_path, bld_id, bld_path FROM gradle.project_mapping")

  val deployQuery = Ignore.readClassResource(classOf[ProjectMapping], "project_mapping.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.project_mapping CASCADE")

  def normalizeProjectName(prjName: String): String = {
    def longestCommonPrefix(list: String*) = list.foldLeft("")((_, _) =>
      (list.min.view, list.max.view).zipped.takeWhile(v => v._1 == v._2).unzip._1.mkString)

    val prjNames = prjName.split(',').toVector
    if (prjNames.size < 2)
      normalizeSingleProjectName(prjName)
    else {
      val normalPrjNames = prjNames.map { normalizeSingleProjectName(_) }
      val prefix = longestCommonPrefix(normalPrjNames: _*)
      prefix + normalPrjNames.map { _.substring(prefix.length)}.mkString(",")
    }
  }

  private def normalizeSingleProjectName(prjName: String): String = {
    val prjNameSegments = prjName.replaceAll("java-com-rocketfuel", "j-c-r").split('-').toVector
    val shortPrjName = prjNameSegments.dropRight(1).mkString("-")

    // TODO handle path with ','
    if (shortPrjName.endsWith(prjNameSegments(prjNameSegments.size - 1).replace('.', '-')))
      shortPrjName else prjNameSegments.mkString("-")
  }

  def projectNames()(implicit connection: Connection): collection.SortedSet[String] = collection.SortedSet(
      list.vector().map { pm => normalizeProjectName(pm.prj_path) }: _*
  )
}
