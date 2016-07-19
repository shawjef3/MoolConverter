package com.rocketfuel.build

import org.apache.logging.log4j.LogManager

trait Logger {

  val logger = LogManager.getLogger(getClass)

}
