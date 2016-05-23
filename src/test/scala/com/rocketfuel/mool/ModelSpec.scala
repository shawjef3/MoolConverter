package com.rocketfuel.mool

import java.nio.file.Paths
import org.scalatest.FunSuite

class ModelSpec extends FunSuite {

  val bld01StringPath = "mool.java.0.1"
  val bld23StringPath = "mool.java.2.3"
  val bld45StringPath = "mool.java.4.5"

  val bld01Path = Bld.absolutePath(bld01StringPath)
  val bld23Path = Bld.absolutePath(bld23StringPath)
  val bld45Path = Bld.absolutePath(bld45StringPath)

  val relCfg0Path = RelCfg.path("0")

  val bld01 = Bld(rule_type = "java_lib", srcs = Some(Vector("01.src")), deps = Some(Vector(bld23StringPath)))
  val bld23 = Bld(rule_type = "java_lib", deps = Some(Vector(bld45StringPath)))
  val bld45 = Bld(rule_type = "java_lib")

  val blds =
    Map(
      bld01Path -> bld01,
      bld23Path -> bld23,
      bld45Path -> bld45
    )

  private val relCfgs =
    Map(
      relCfg0Path -> RelCfg("g", "a", "0.0", `jar-with-dependencies` = Some(RelCfg.Artifact(target = bld01StringPath, artifact_path = "a")))
    )

  val model =
    Model(
      root = Paths.get("."),
      blds = blds,
      relCfgs = relCfgs,
      versions = Map.empty
    )

  test("relCfgsToBld") {
    val expected =
      Map(relCfg0Path -> Some(bld01Path))
    assertResult(expected)(model.relCfgsToBld)
  }

  test("bldsToBlds") {
    val expected =
      Map(
        bld01Path -> Set(bld23Path),
        bld23Path -> Set(bld45Path),
        bld45Path -> Set()
      )
    assertResult(expected)(model.bldsToBlds)
  }

  test("bldToBldsTransitive") {
    val expected =
      Map(
        bld01Path -> Set(bld23Path, bld45Path),
        bld23Path -> Set(bld45Path),
        bld45Path -> Set()
      )
    assertResult(expected)(model.bldToBldsTransitive)
  }

  test("relCfgsToBldsTransitive") {
    val expected =
      Map(relCfg0Path -> Set(bld01Path, bld23Path, bld45Path))
    assertResult(expected)(model.relCfgsToBldsTransitive)
  }

  test("bldsToRelCfgs") {
    val expected =
      Map(
        bld01Path -> Set(relCfg0Path),
        bld23Path -> Set(),
        bld45Path -> Set()
      )
    assertResult(expected)(model.bldsToRelCfgs)
  }

  test("bldsToRelCfgsTransitive") {
    val expected =
      Map(
        bld01Path -> Set(relCfg0Path),
        bld23Path -> Set(relCfg0Path),
        bld45Path -> Set(relCfg0Path)
      )
    assertResult(expected)(model.bldsToRelCfgsTransitive)
  }

  test("01.src") {
    val bld01 = blds(bld01Path)
    val expected =
      Vector(model.root.resolve("java").resolve("0").resolve("1").resolve("01.src"))
    val actual = bld01.srcPaths(model, bld01Path)
    assertResult(expected)(actual)
  }

  test("circular dependencies") {
    val circularBlds =
      Map(
        bld01Path -> bld01,
        bld23Path -> bld23,
        bld45Path -> bld45.copy(deps = Some(Vector(bld01StringPath)))
      )

    val circularModel =
      model.copy(
        blds = circularBlds
      )

    val expected =
      Map(relCfg0Path -> Set(bld01Path, bld23Path, bld45Path))
    val actual =
      circularModel.relCfgsToBldsTransitive

    assertResult(expected)(actual)
  }

}
