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
  "org.apache.logging.log4j" % "log4j-api" % "2.6.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.6.1",
  //testing
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.google.jimfs" % "jimfs" % "1.1" % "test",
  "com.github.nikita-volkov" % "sext" % "0.2.4" % "test"
)
