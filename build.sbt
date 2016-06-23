name := "mool-conversion"

organization := "com.rocketfuel.build.mool"

version := "0.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  //json
  "io.argonaut" %% "argonaut" % "6.1a",
  "org.scalaz" %% "scalaz-core" % "7.2.4",
  //logging
  "org.slf4s" %% "slf4s-api" % "1.7.13",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.5",
  //testing
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.google.jimfs" % "jimfs" % "1.1" % "test",
  "com.github.nikita-volkov" % "sext" % "0.2.4" % "test"
)
