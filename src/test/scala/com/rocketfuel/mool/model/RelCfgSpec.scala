package com.rocketfuel.mool.model

import com.rocketfuel.mool.RelCfg
import org.scalatest.FunSuite
import scalaz.Scalaz._

class RelCfgSpec
  extends FunSuite {

  test("can parse relcfg_example") {
    val relCfg = RelCfg.of(getClass.getResourceAsStream("relcfg_example"))

    assert(relCfg.contains("common.message"))

    val common = relCfg("common.message")

    assertResult("com.rocketfuel.common.message")(common.group_id)
    assertResult("common.message")(common.artifact_id)
    assertResult("102.16.0")(common.base_version)
    assertResult(
      RelCfg.Artifact("mool.java.com.rocketfuel.common.message.CommonMessageNoDeps", "java/com/rocketfuel/common/message/CommonMessageNoDeps.jar").some
    )(common.`jar-no-dependencies`
    )
    assertResult(RelCfg.Artifact("mool.java.com.rocketfuel.common.message.CommonMessage", "java/com/rocketfuel/common/message/CommonMessage.jar").some
    )(common.`jar-with-dependencies`
    )
  }

  test("can parse relcfg_example2") {
    val relCfg = RelCfg.of(getClass.getResourceAsStream("relcfg_example2"))

    assert(Set("test_rule_with_all_sections", "test_rule_with_deployment_section").forall(relCfg.contains))
  }

}
