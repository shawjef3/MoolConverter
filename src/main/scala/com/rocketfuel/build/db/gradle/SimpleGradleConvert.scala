package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.gradle.GradleConvert.loadResource
import com.rocketfuel.build.db.mvn._
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.sdbc.PostgreSql._

object SmallProjectFilter extends (String => Boolean) {
  def apply(path: String): Boolean = {
    SmallProjectFilter.exact.contains(path) ||
      SmallProjectFilter.prefixes.exists(prefix => path.startsWith(prefix))
  }

  val prefixes =
    Set(
      "server-util",
      "server-geoip",
      "camus",
      "common-message",
      "grid-quasar",
      "grid-common-spark-Spark",
    )

  val quasarDeps: Set[String] =
    Set(
      "3rd_party-java-com-google-protobuf-ZeroCopyByteString",
      "3rd_party-java-com-googlecode-protobuf-pro-duplex-DuplexLogProto",
      "3rd_party-java-com-googlecode-protobuf-pro-duplex-DuplexProtobufAll",
      "3rd_party-java-mvn-com-google-guava-GuavaTestLibAll",
      "3rd_party-java-mvn-org-apache-curator-CuratorAll",
      "3rd_party-java-mvn-org-apache-curator-CuratorTestAll",
      "3rd_party-java-mvn-org-codehaus-jackson-JacksonAll",
      "3rd_party-java-mvn-redis-clients-JedisAll",
      "common-rpcutils-DuplexProtocolJavaProto",
      "common-rpcutils-EmptyJavaProto",
      "common-rpcutils-RpcTestJavaProto",
      "dp-luke-LookupJavaProtos",
      "dp-luke-PageJavaProtos",
      "ei-common-Cache",
      "ei-common-RpcClient",
      "ei-common-RpcServer",
      "grid-common-metrics-collectors-MetricsCollector",
      "grid-common-metrics-reporters-AllReporters",
      "grid-common-spark-SparkCommon",
      "grid-common-testutils-TestLog4jAppendConsole",
      "grid-common-utils-FileSystemUtil",
      "grid-dmp-ssvadapter-utils-HdfsUtilsLib",
      "grid-luke-service-api-ServiceApi",
      "grid-luke-service-api-TestUtil",
      "grid-luke-service-api-chaining-ChainedTables",
      "grid-luke-service-client-ClientConf",
      "grid-luke-service-client-LookupClient",
      "grid-luke-service-client-LookupClientNoConf",
      "grid-luke-service-client-LookupRpcClient",
      "grid-luke-service-core-Client",
      "grid-luke-service-core-ClientTest",
      "grid-luke-service-core-L1Server",
      "grid-luke-service-core-L2Server",
      "grid-luke-service-core-LoggingConnectionEventListener",
      "grid-luke-service-core-LukeServer",
      "grid-luke-service-core-MiniCluster",
      "grid-luke-service-core-common-ByteSerDe",
      "grid-luke-service-core-common-ControlValue",
      "grid-luke-service-core-common-prod_conf-ServiceConfig",
      "grid-luke-service-core-common-PageClient",
      "grid-luke-service-core-common-PageStore",
      "grid-luke-service-core-common-RpcClientUtil",
      "grid-luke-service-core-common-ServiceConfig",
      "grid-luke-service-core-common-SizeCalculator",
      "grid-luke-service-core-common-Stringifier",
      "grid-luke-service-core-lookupservice-LookupService",
      "grid-luke-service-core-lookupservice-RemotePageStore",
      "grid-luke-service-core-pageservice-PageService",
      "grid-luke-service-core-pageservice-RedisPageStore",
      "grid-luke-service-discovery-ServiceDiscovery",
      "grid-luke-service-exception-LukeException",
      "grid-luke-service-metrics-Common",
      "grid-luke-service-metrics-LookupServiceMetrics",
      "grid-luke-service-metrics-LookupRpcClientMetrics",
      "grid-luke-service-metrics-RedisPageStoreMetrics",
      "grid-luke-squeeze-payload-block-BlockerApi",
      "grid-luke-squeeze-payload-block-PayloadBlockerLib",
      "grid-luke-squeeze-payload-pagified-PagifiedPayloadApi",
      "grid-luke-squeeze-payload-pagified-PagifiedPayloadLib",
      "grid-luke-squeeze-payload-PayloadApi",
      "grid-luke-squeeze-payload-PayloadLib",
      "grid-luke-squeeze-payload-PayloadTestLib",
      "grid-luke-utils-Bytes",
      "grid-luke-utils-DataSize",
      "grid-quasar-config-Config",
      "grid-quasar-config-ConfigTypes",
      "grid-quasar-io-IO",
      "grid-quasar-lookup-Lookup",
      "grid-quasar-metric-Metric",
      "grid-quasar-process-Process",
      "grid-quasar-resources-conf-report_cfg",
      "grid-quasar-schema-Schema",
      "grid-quasar-schema-SchemaPojos",
      "grid-scrubplus-logformat-generated-pojo-GeneratedPojoLib",
      "grid-scrubplus-logformat-generated-proto_pojo-GeneratedProtoPojoLib",
      "3rd_party-java-mvn-org-json4s-Json4sAll2_11",
      "grid-common-spark-SparkCatalyst2_0",
      "grid-common-spark-SparkCore2_0"
    )

  val exact =
    Set(
      "grid-scrubplus-logformat-generated-hive_proto-EvfColumnsProto",
      "3rd_party-java-mvn-org-apache-hadoop-HadoopAll2",
      "3rd_party-java-mvn-org-ostermiller-Utils"
    ) ++ quasarDeps
}

object SimpleGradleConvert extends Logger {

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = GradleCopy.all.vector().map(GradleCopy.toCopy(_)).toSet
    Copy.copy(copies, moolRoot, destinationRoot)
  }

  def builds(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val projectsRoot = destinationRoot.resolve("projects")

    val identifiers = {
      for (i <- Identifier.list.iterator()) yield {
        i.bldId -> i
      }
    }.toMap

    val dependencies =
      com.rocketfuel.build.db.mvn.Dependency.list.vector().groupBy(_.sourceId)

    val exclusions = Exclusion.byBldIdAndDependencyId()

    val localBlds = Bld.locals.vector()
    val modulePaths = localBlds.foldLeft(Map.empty[Int,String]) { case (m, bld) =>
        m + (bld.id -> Projects.pathToModulePath(bld.path))
    }
    val moduleBlds = localBlds.groupBy { bld => Projects.pathToModulePath(bld.path) }

    val moduleOutputs = localBlds.foldLeft(Map.empty[String, Int]) { case (moduleOuts, bld) =>
      val identifier = identifiers(bld.id)
      val output = s"${identifier.groupId}:${identifier.artifactId}:${identifier.version}"
      moduleOuts + (output -> bld.id)
    }

    val convertor = new GradleConvert(projectsRoot, modulePaths, moduleOutputs)

    val includedBuilds =
      for {
        (path, blds) <- moduleBlds
        if SmallProjectFilter(path)
      } yield {
        val bldsWithDeps =
          blds
          .map { bld => (bld, dependencies.getOrElse(bld.id, Vector.empty))}
          .toMap

        val modulePath = projectsRoot.resolve(path)
        val gradle = convertor.gradle(path, bldsWithDeps, modulePath, exclusions)
        val gradlePath = modulePath.resolve("build.gradle")

        Files.createDirectories(modulePath)
        Files.write(gradlePath, gradle.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        (path, blds.map(_.path.mkString("-")))
      }

    val settingsGradle = destinationRoot.resolve("settings.gradle")
    val settings = includedBuilds.toSeq.sortBy {_._1}.foldLeft("") { (buffer, prjNames) =>
      val comment = if (prjNames._1 == prjNames._2) "" else s" // ${prjNames._2}"
      buffer + s"include ':${prjNames._1}'$comment\n"
    }

    Files.write(settingsGradle,
      (settings + loadResource("settings_end.gradle")).getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)

  }
}
