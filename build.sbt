name := "mool-conversion"

organization := "com.rocketfuel.mool"

version := "0.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  //json
  "io.argonaut" %% "argonaut" % "6.1",
  //logging
  "org.slf4s" %% "slf4s-api" % "1.7.13",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.5",
  //testing
  "org.scalatest" %% "scalatest" % "2.2.6"
)
