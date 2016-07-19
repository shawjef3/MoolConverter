package com.rocketfuel.build.mool

import org.scalatest.FunSuite

class ConfigurationsSpec extends FunSuite {

  test("adding main replaces test") {
    val c0 = Configurations(test = Set(1))
    val c1 = c0.withMains(1)

    assertResult(Configurations(main = Set(1)))(c1)
  }

  test("adding test keeps main") {
    val c0 = Configurations(main = Set(1))
    val c1 = c0.withTests(1)

    assertResult(Configurations(main = Set(1)))(c1)
  }

  test("adding tests selectively adds to tests") {
    val c0 = Configurations(Set(1,2,3), Set(4,5,6))
    val c1 = c0 ++ Configurations(main = Set(4,9), test = Set(3, 7, 8))

    assertResult(Configurations(Set(1,2,3,4,9), Set(5,6,7,8)))(c1)
  }

}
