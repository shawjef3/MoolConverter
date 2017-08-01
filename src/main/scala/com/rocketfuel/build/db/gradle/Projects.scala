package com.rocketfuel.build.db.gradle

object Projects {

  def pathToModulePath(path: Seq[String]): String = {
    val patchedPath = if (path.head == "grid") "grid2" +: path.drop(1)
    else if (path.take(3) == Seq("java", "com", "rocketfuel")) path.drop(3)
    else if (path.take(3) == Seq("clojure", "com", "rocketfuel")) "clojure" +: path.drop(3)
    else if (path.head == "java") "3rd_party" +: path
    else path

    stripSuffices(patchedPath.mkString("-"))
  }

  private def stripSuffices(s: String) : String = {
    val short = s.stripSuffix("Test")
      .stripSuffix("Pkg")
      .stripSuffix("NoDeps")
      // .stripSuffix("Lib")
      .stripSuffix("_bin")
      .stripSuffix("_test")
      .stripSuffix("_lib")
    if (s.endsWith("-ModulesTest") || // circular dep m/a/core/modules/Modules m/a/core/workflow/WorkFlow
      short.isEmpty) s
    else short
  }
}
