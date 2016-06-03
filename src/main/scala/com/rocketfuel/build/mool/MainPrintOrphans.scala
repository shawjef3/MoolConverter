package com.rocketfuel.build.mool

object MainPrintOrphans extends ModelApp {

  model.bldOrphans.foreach(println)
  model.testBldOrphans.foreach(println)

}
