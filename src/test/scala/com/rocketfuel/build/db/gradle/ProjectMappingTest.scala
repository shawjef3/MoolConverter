package com.rocketfuel.build.db.gradle

import org.scalatest.FunSuite

class ProjectMappingTest extends FunSuite {

  test("testNormalizeProjectName") {
    assert(ProjectMapping.normalizeProjectName("dp-quasar-quasar") === "dp-quasar")
    assert(ProjectMapping.normalizeProjectName("java-com-rocketfuel-grid-retention-grid.retention") === "j-c-r-grid-retention")
//    assert(ProjectMapping.normalizeProjectName("java-com-rocketfuel-grid-datascrub-grid.scrub") === "java-com-rocketfuel-grid-datascrub")

    assert(ProjectMapping.normalizeProjectName("java-com-rocketfuel-grid-foo,java-com-rocketfuel-grid-bar") ===
      "j-c-r-grid-foo,bar")
  }
}
