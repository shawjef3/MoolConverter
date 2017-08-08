package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.mool.Bld

case class BuildGradleParts(
  compileOnlyDeps: Set[String] = Set.empty,
  compileDeps: Set[String] = Set.empty,
  compileTestDeps: Set[String] = Set.empty,
  plugins: Set[String] = Set("plugin: 'java'"),
  snippets: Set[String] = Set.empty
)

object BuildGradleParts extends Logger {
  def valueOf(
    prjBld: Bld,
    dependencyList: Set[String]
  ): BuildGradleParts = {
    prjBld.ruleType match {
      case "java_proto_lib" =>
        BuildGradleParts(compileDeps = Set(protoLib) ++ dependencyList,
          plugins = Set("plugin: 'java'", "plugin: 'com.google.protobuf'"),
          snippets = Set(protoConfigSnippet))
      case "java_lib" | "file_coll" =>
        BuildGradleParts(compileDeps = dependencyList,
          plugins = Set("plugin: 'java'"),
          snippets = Set(sourceCompatibility(prjBld.javaVersion)))
      case "java_bin" =>
        BuildGradleParts(compileDeps = dependencyList,
          plugins = Set("plugin: 'java'", "plugin: 'com.github.johnrengelman.shadow'"),
          snippets = shadowJarConfig(prjBld.mainClass).toSet + sourceCompatibility(prjBld.javaVersion))
      case "java_test" =>
        // 'from: "${' will be interpolated by Gradle
        BuildGradleParts(plugins = Set("plugin: 'java'", "from: \"${rootProject.projectDir}/gradle/tests.gradle\""),
          snippets = Set(testNGConfig(prjBld.testGroups)) + sourceCompatibility(prjBld.javaVersion),
          compileDeps = dependencyList)
      case "java_thrift_lib" =>
        BuildGradleParts(plugins = Set("plugin: 'java'", "plugin: 'org.jruyi.thrift'"),
          snippets = Set(thriftConfigSnippet) + sourceCompatibility(prjBld.javaVersion),
          compileDeps = Set(thriftLib) ++ dependencyList)
      case "scala_lib" =>
        val compileDeps = prjBld.scalaVersion match {
          case Some("2.10") => scala210Libs.toSet ++ dependencyList
          case Some("2.11") => scala211Libs.toSet ++ dependencyList
          case Some("2.12") => dependencyList // TODO should have 2.12 libs
          case _ =>
            logger.warn(s"scala_lib with unknown version ${prjBld}")
            dependencyList
        }
        BuildGradleParts(compileDeps = compileDeps,
          plugins = Set("plugin: 'scala'"),
          snippets = Set(sourceCompatibility(prjBld.javaVersion)))
      case "scala_test" =>
        val compileDeps = prjBld.scalaVersion match {
          case Some("2.10") => scala210Libs.toSet ++ dependencyList + scalatestLibs
          case Some("2.11") => scala211Libs.toSet ++ dependencyList + scalatestLibs
          case Some("2.12") => dependencyList + scalatestLibs // TODO should have 2.12 libs
          case _ =>
            logger.warn(s"scala_test with unknown version ${prjBld}")
            dependencyList + scalatestLibs
        }
        BuildGradleParts(compileDeps = compileDeps,
          plugins = Set("plugin: 'scala'", "plugin: 'com.github.maiflai.scalatest'"),
          snippets = Set(sourceCompatibility(prjBld.javaVersion), scalatestSnippet))
      case "scala_bin" =>
        val compileDeps = prjBld.scalaVersion match {
          case Some("2.10") => scala210Libs.toSet ++ dependencyList
          case Some("2.11") => scala211Libs.toSet ++ dependencyList
          case Some("2.12") => dependencyList // TODO should have 2.12 libs
          case _ =>
            logger.warn(s"scala_lib with unknown version ${prjBld}")
            dependencyList
        }
        BuildGradleParts(compileDeps = compileDeps,
          plugins = Set("plugin: 'scala'", "plugin: 'com.github.johnrengelman.shadow'"),
          snippets = shadowJarConfig(prjBld.mainClass).toSet + sourceCompatibility(prjBld.javaVersion))
      case _ =>
        BuildGradleParts(plugins = Set("plugin: 'base'"))
    }
  }

  // not s""": leave interpolation to Groovy/Gradle
  private val protoConfigSnippet =
    """
      |protobuf {
      |  protoc {
      |    path = "${System.env.HOME}/.mooltool/packages/protobuf/bin/protoc"
      |  }
      |}
      |task sourcesJar(type: Jar, dependsOn: classes) {
      |    classifier = 'sources'
      |    from sourceSets.main.allSource
      |    from sourceSets.main.proto
      |    from "${protobuf.generatedFilesBaseDir}/main/java"
      |}
      |idea {
      |    module {
      |        sourceDirs += file("${protobuf.generatedFilesBaseDir}/main/java");
      |    }
      |}
      |""".stripMargin
  private val thriftConfigSnippet =
    """
      |compileThrift {
      |  thriftExecutable "${System.env.HOME}/.mooltool/packages/thrift-0.9.1/bin/thrift"
      |}
      |""".stripMargin
  private val testNGConfigSnippetWithGroupsPre =
    """
      |test {
      |  useTestNG() {
      |    environment 'BUILD_ROOT', "${rootProject.projectDir}/projects/testdata"
      |    includeGroups """.stripMargin
  private val testNGConfigSnippetWithGroupsPost =
    """
      |  }
      |}
      |""".stripMargin

  def testNGConfig(testGroups: Option[String]): String = {
    testNGConfigSnippetWithGroupsPre +
      testGroups.getOrElse("unit").split(",").map {
        "'" + _ + "'"
      }.mkString(", ") +
      testNGConfigSnippetWithGroupsPost
  }

  private val shadowJarSnippet =
    """shadowJar {
      |  manifest {
      |    attributes 'Main-Class': '__MAIN_CLASS__'
      |  }
      |}
      |""".stripMargin

  def shadowJarConfig(mainClass: Option[String]): Option[String] =
    mainClass.map(mClz => shadowJarSnippet.replace("__MAIN_CLASS__", mClz))

  private val scala210Libs = List("  compile 'org.scala-lang:scala-library:2.10.4'",
    "  compile 'org.scala-lang:scala-actors:2.10.4'"
  )
  private val scala211Libs = List("  compile 'org.scala-lang:scala-library:2.11.8'")
  // use 1.4.2 with scalatest 3, now stick to 1.1
  private val scalatestLibs = "  testRuntime 'org.pegdown:pegdown:1.1.0'"
  private val scalatestSnippet =
    """
      |test {
      |    maxParallelForks = 1
      |    environment 'BUILD_ROOT', "${rootProject.projectDir}/projects/testdata"
      |}
    """.stripMargin
  private val scala210Tasks = "rootProject.tasks.build210.dependsOn tasks.build\n"
  private val scala211Tasks = "rootProject.tasks.build211.dependsOn tasks.build\n"

  private val protoLib = "  compile files(\"${System.env.HOME}/.mooltool/packages/protobuf/java/target/protobuf-2.5.0.jar\")"
  private val thriftLib = "  compile 'org.apache.thrift:libthrift:0.9.1'"

  def sourceCompatibility(javaVersion: Option[String]): String =
    "sourceCompatibility = " + javaVersion.getOrElse("1.7")

}