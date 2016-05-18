package com.rocketfuel.mool

import argonaut._
import java.io.InputStream
import java.nio.file.{Files, Path}

/**
  *
  * @param rule_type
  * @param srcs
  * @param deps
  * @param compileDeps
  * @param scala_version
  * @param maven_specs is for a dependency to be downloaded.
  */
case class Bld(
  rule_type: String,
  srcs: Option[Vector[String]],
  deps: Option[Vector[String]],
  compileDeps: Option[Vector[String]],
  scala_version: Option[String],
  maven_specs: Option[Bld.MavenSpecs]
)

object Bld {

  def of(file: Path): Map[String, Bld] = {
    of(Files.newInputStream(file))
  }

  def of(stream: InputStream): Map[String, Bld] = {
    val fileString =
      io.Source.fromInputStream(stream).getLines().map(_.trim).filter(! _.startsWith("#")).mkString

    val decodeResult =
      Parse.decode[Map[String, Bld]]("{" + fileString + "}")

    decodeResult.toOption.get
  }

  case class MavenSpecs(
    artifact_id: String,
    group_id: String,
    version: String,
    repo_url: String
  )

  object MavenSpecs {
    implicit val decodeJson: DecodeJson[MavenSpecs] =
      DecodeJson.derive[MavenSpecs]
  }

  implicit val decodeJson: DecodeJson[Bld] =
    DecodeJson.derive[Bld]

}
