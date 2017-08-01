package com.rocketfuel.build.db.gradle

case class BldGrouping(sharedPrefix: String,
                       excludes: Set[String],
                       gradleProjectName: String)

object Projects {

  val bldGroupings = Set(
    // merge all common protobufs
    // temporarily split aerospike_data_message & page_context
    BldGrouping(sharedPrefix = "common-message", excludes = Set("common-message-protobuf-AerospikeDataMessageProto"),
      gradleProjectName = "common-message"),
    // create one project for server.util, the only external dependency is server.geoip.TimeZone
    BldGrouping(sharedPrefix = "server-util", excludes = Set.empty,
      gradleProjectName = "server-util"),
    BldGrouping(sharedPrefix = "server-geoip-TimeZone", excludes = Set.empty,
      gradleProjectName = "server-util"),

    BldGrouping(sharedPrefix = "grid-quasar", excludes = Set.empty,
      gradleProjectName = "grid-quasar")
  )
  def pathToModulePath(path: Seq[String]): String = {
    val patchedPath = if (path.head == "grid") "grid2" +: path.drop(1)
    else if (path.take(3) == Seq("java", "com", "rocketfuel")) path.drop(3)
    else if (path.take(3) == Seq("clojure", "com", "rocketfuel")) "clojure" +: path.drop(3)
    else if (path.head == "java") "3rd_party" +: path
    else path

    val remappedPath = bldGroupings.foldLeft(patchedPath.mkString("-")) { (path, bldGrouping) =>
      if (path.startsWith(bldGrouping.sharedPrefix) && bldGrouping.excludes.contains(path)) bldGrouping.gradleProjectName
      else path
    }
    stripSuffices(stripSuffices(remappedPath))
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
