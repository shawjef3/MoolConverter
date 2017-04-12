name := "mool-conversion"

organization := "com.rocketfuel.build.mool"

version := "0.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "io.argonaut" %% "argonaut" % "6.2",//json
  "org.scalaz" %% "scalaz-core" % "7.2.10",
  "com.rocketfuel.sdbc" %% "postgresql-jdbc" % "2.0.2",//database
  //logging
  "org.apache.logging.log4j" % "log4j-api" % "2.6.2",
  "org.apache.logging.log4j" % "log4j-core" % "2.6.2",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  //testing
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.google.jimfs" % "jimfs" % "1.1" % "test",
  "com.github.nikita-volkov" % "sext" % "0.2.6" % "test"
)

//scalacOptions := Seq("-Xlog-implicits")
