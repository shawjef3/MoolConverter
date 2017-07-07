package com.rocketfuel.build.mool

import argonaut._
import java.io.InputStream
import java.nio.file.{Files, Path}

case class Bld(
  rule_type: String,
  srcs: Option[Vector[String]] = None,
  deps: Option[Vector[String]] = None,
  compileDeps: Option[Vector[String]] = None,
  scala_version: Option[String] = None,
  java_version: Option[String] = None,
  maven_specs: Option[Bld.MavenSpecs] = None,
  package_modules: Option[Vector[String]] = None,
  package_tests: Option[Vector[String]] = None,
  file_package: Option[String] = None,
  extract_deps: Option[Vector[String]] = None
) {

  def language: String =
    Bld.language(rule_type)

  def srcPaths(model: Model, bldPath: MoolPath): Vector[Path] =
    srcPaths(model.root, bldPath)

  def srcPaths(root: Path, bldPath: MoolPath): Vector[Path] = {
    val bldDir = bldPath.foldLeft(root)(_.resolve(_))
    for {
      src <- srcs.getOrElse(Vector.empty)
    } yield bldDir.resolveSibling(src)
  }

  def depPaths(bldPath: MoolPath): Vector[MoolPath] =
    for {
      dep <- deps.getOrElse(Vector.empty)
    } yield Bld.relativePath(bldPath, dep)

  def compileDepPaths(bldPath: MoolPath): Vector[MoolPath] =
    for {
      compileDep <- compileDeps.getOrElse(Vector.empty)
    } yield Bld.relativePath(bldPath, compileDep)

  def extractDepPaths(bldPath: MoolPath): Vector[MoolPath] =
    for {
      extractDep <- extract_deps.getOrElse(Vector.empty)
    } yield Bld.relativePath(bldPath, extractDep)

}

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

  /**
    * Split a string by '.', and drop the leading "mool" element.
    *
    * If the pathString starts with a '.', it is relative, and so it
    * is appended to the localMoolPath.
    *
    * @param pathString
    * @return
    */
  def relativePath(localMoolPath: MoolPath, pathString: String): Vector[String] = {
    if (pathString.head == '.') localMoolPath.dropRight(1) :+ pathString.tail
    else {
      val split = pathString.split('.').toVector
      split.drop(1)
    }
  }

  /**
    * Split by '.', and drop the leading "mool".
    * @param pathString
    * @return
    */
  def absolutePath(pathString: String): MoolPath = {
    val split = pathString.split('.').toVector
    split.drop(1)
  }

  def language(ruleType: String): String =
    ruleType match {
      case "release_package" | "file_coll" =>
        "resources"
      case "java_proto_lib" =>
        "proto"
      case "java_lib" | "java_test" | "java_bin" =>
        "java"
      case "scala_lib" | "scala_test" | "scala_bin" =>
        "scala"
    }

  case class MavenSpecs(
    artifact_id: String,
    group_id: String,
    version: String,
    repo_url: String,
    classifier: Option[String]
  )

  object MavenSpecs {
    implicit val decodeJson: DecodeJson[MavenSpecs] =
      DecodeJson.derive[MavenSpecs]
  }

  implicit val decodeJson: DecodeJson[Bld] =
    DecodeJson.derive[Bld]

}
