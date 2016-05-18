package com.rocketfuel.mool.model

import com.rocketfuel.mool.Bld
import org.scalatest.FunSuite

class BldSpec
  extends FunSuite {

  test("can parse bld_example") {
    val bld = Bld.of(getClass.getResourceAsStream("bld_example"))

    assert(bld.contains("ServerProtoAll"))

    val serverProtoAll = bld("ServerProtoAll")

    assert(serverProtoAll.rule_type == "java_lib")

    assertResult(7)(serverProtoAll.deps.map(_.size).getOrElse(0))
  }

  test("can parse bld_example2") {
    val bld = Bld.of(getClass.getResourceAsStream("bld_example2"))

    assertResult(12)(bld.size)
  }

  test("can parse bld_mvn_example") {
    val bld = Bld.of(getClass.getResourceAsStream("bld_mvn_example"))

    assert(bld.contains("CommonsCollections"))

    val commonsCollections = bld("CommonsCollections")

    assertResult("java_lib")(commonsCollections.rule_type)
    assert(commonsCollections.maven_specs.isDefined)

    val mavenSpecs = commonsCollections.maven_specs.get

    assertResult("commons-collections")(mavenSpecs.artifact_id)
    assertResult("commons-collections")(mavenSpecs.group_id)
    assertResult("http://nexus.rfiserve.net/content/groups/public")(mavenSpecs.repo_url)
    assertResult("3.2.1")(mavenSpecs.version)
  }

}
