package com.rocketfuel.build.db.gradle

case class BldGrouping(sharedPrefix: String,
                       excludes: Set[String] = Set.empty,
                       gradleProjectName: Option[String] = None)

object Projects {

  private val bldGroupings = Seq(
    BldGrouping(sharedPrefix = "camus-api"),
    BldGrouping(sharedPrefix = "camus-coders"),
    BldGrouping(sharedPrefix = "camus-etl-mapred-support"),
    BldGrouping(sharedPrefix = "camus-etl"),
    BldGrouping(sharedPrefix = "camus-schemaregistry"),
//    BldGrouping(sharedPrefix = "camus"),
    // merge all common protobufs
    // temporarily split aerospike_data_message & page_context
    BldGrouping(sharedPrefix = "common-message", excludes = Set("common-message-protobuf-AerospikeDataMessageProto")),
    BldGrouping(sharedPrefix = "grid-scrubplus-logformat-generated-hive_proto-EvfColumnsProto",
      gradleProjectName = Some("common-message")),

    // create one project for server.util, the only external dependency is server.geoip.TimeZone
    BldGrouping(sharedPrefix = "server-util"),
    BldGrouping(sharedPrefix = "server-geoip-TimeZone"),

    BldGrouping(sharedPrefix = "server-geoip"),
    BldGrouping(sharedPrefix = "grid-quasar")
  )

  def pathToModulePath(path: Seq[String]): String = {
    val patchedPath = if (path.head == "grid") "grid2" +: path.drop(1)
    else if (path.take(3) == Seq("java", "com", "rocketfuel")) path.drop(3)
    else if (path.take(3) == Seq("clojure", "com", "rocketfuel")) "clojure" +: path.drop(3)
    else if (path.head == "java") "3rd_party" +: path
    else path

    val defaultPath = patchedPath.mkString("-")
    val remappedPath = bldGroupings.find { bldGroup =>
      defaultPath.startsWith(bldGroup.sharedPrefix) && !bldGroup.excludes.contains(defaultPath)
    }.map { bldGroup => bldGroup.gradleProjectName.getOrElse(bldGroup.sharedPrefix)
    }.getOrElse(defaultPath)

    stripSuffices(remappedPath)
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
