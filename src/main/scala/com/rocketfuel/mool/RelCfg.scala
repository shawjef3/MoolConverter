package com.rocketfuel.mool

import argonaut._
import java.io.InputStream
import java.nio.file.{Files, Path}

case class RelCfg(
  group_id: String,
  artifact_id: String,
  base_version: String,
  `jar-no-dependencies`: Option[RelCfg.Artifact] = None,
  `jar-with-dependencies`: Option[RelCfg.Artifact] = None
) {

}

object RelCfg {

  def of(file: Path): Map[String, RelCfg] = {
    of(Files.newInputStream(file))
  }

  def of(stream: InputStream): Map[String, RelCfg] = {
    val fileString =
      io.Source.fromInputStream(stream).getLines().map(_.trim).filter(! _.startsWith("#")).mkString

    stream.close()

    val decodeResult =
      Parse.decode[Map[String, RelCfg]]("{" + fileString + "}")

    decodeResult.toOption.get
  }

  /**
    * Split a string by '.'. RelCfgs don't start with "mool", so unlike with BLDs,
    * there is no need to drop the first element.
    * @param pathString
    * @return
    */
  def path(pathString: String): Vector[String] = {
    pathString.split('.').toVector
  }

  case class Artifact(
    target: String,
    artifact_path: String
  ) {
    val targetPath: MoolPath =
      //we're lucky that targets are always absolute paths, so we can use Bld.absolutePath.
      Bld.absolutePath(target)
  }

  object Artifact {
    implicit val decodeJson: CodecJson[Artifact] =
      CodecJson.derive[Artifact]
  }

  implicit val codecJson: CodecJson[RelCfg] =
    CodecJson.casecodec5(apply, unapply)("group_id", "artifact_id", "base_version", "jar-no-dependencies", "jar-with-dependencies")
}
