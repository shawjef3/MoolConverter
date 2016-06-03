package com.rocketfuel.build.mool

object MainPrintConflicts extends ModelApp {

  model.bldIndirectConflicts.foreach(println)

  println("direct")

  model.bldDirectConflicts.foreach(println)

}
