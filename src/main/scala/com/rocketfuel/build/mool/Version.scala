package com.rocketfuel.build.mool

import java.nio.file.{Files, Path}

case class Version(
  artifactId: String,
  commit: String,
  version: String
) {
  def compareTo(other: Version): Int = {
    version.iterator.map(Some(_)).zipAll(other.version.iterator.map(Some(_)), None, None).map {
      case (Some(mine), Some(theirs)) =>
        mine.compareTo(theirs)
      case (None, _) =>
        1
      case (_, None) =>
        -1
    }.find(_ != 0).getOrElse(0)
  }
}

object Version {
  def ofCsv(line: String): Version = {
    val split = line.split(',')
    val groupId = split(0).trim
    val commit = split(1).trim
    val version = split(2).trim
    Version(groupId, commit, version)
  }

  def ofFile(file: Path): Set[Version] = {
    val inStream = Files.newInputStream(file)
    val fileContents = io.Source.fromInputStream(inStream).getLines().filter(_.count(_ == ',') == 2)
    fileContents.map(ofCsv).toSet
  }

  implicit val ordering: Ordering[Version] =
    new Ordering[Version] {
      override def compare(
        x: Version,
        y: Version
      ): Int = x.compareTo(y)
    }
}
