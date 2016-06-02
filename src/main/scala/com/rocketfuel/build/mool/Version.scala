package com.rocketfuel.build.mool

import java.nio.file.{Files, Path}

case class Version(
  groupId: String,
  commit: String,
  version: String
)

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
}
