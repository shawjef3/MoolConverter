Run
===

Set up a local PostgreSQL instance. For MacOS, an easy way is to use <http://postgresapp.com/>. Use it to initialize and start a new instance. Double click on one of the running databases to connect to it, and run,

```postgresql
CREATE DATABASE mool_conversion;
```

The locations for the mool repo and converted repo are hard-coded.

mool repo: `~/git/data/vostok`

converted repo: `/tmp/mool-conversion`

There are three main classes.

* com.rocketfuel.build.db.MainDeploy creates the tables and views to hold the mool repository and views for the converted project. It then reads the mool repository and writes it to the database.
* com.rocketfuel.build.db.MainConvert reads the views that represent the Maven repository, creates POM files, and copies source files.
* com.rocketfuel.build.db.MainPoms reads the Maven models from the database and only creates POM files.

Development
===========

Run `MainDeploy`. Using a database client of your choice, connect to the `mool_conversion` database. Create functions and views to your heart's content. When you are happy, you can add them to part of the deployment process. You can see the existing ones in [mvn](src/main/resources/com/rocketfuel/build/db/mvn) and [Deploy.scala](src/main/scala/com/rocketfuel/build/db/Deploy.scala). If you are adding a new target project type, you should create a new schema in the database, along with a sibling package to the `mvn` package, e.g. `com.rocketfuel.build.db.gradle`.

Import Converted Project in IntelliJ
====================================

The initial import requires a lot of memory and time. Expect about 15 minutes. After the initial import, pom changes import quickly (a few seconds). If you ask to have sources downloaded, that can take much longer.

In IntelliJ settings, Build, Execution, Deployment/Build Tools/Maven/Importing, set `VM options for importer` to `-Xmx4g`.

It also helps to increase IntelliJ's memory. `Help/Edit Custom VM Options...` Replace `-Xmx2g` with `-Xmx6g`.

IntelliJ won't generate Java files for Protobuf or Thrift files. Run the following commands in the project root.

```
mvn --fail-at-end -am -pl :server.rfi.bidder.adx.RealTimeBiddingProto,:grid.datascrub.schema.ImpressionDraftRecordJavaProto,:rpcutils.EmptyJavaProto,:common.message.protobuf.reporting.DMPProfileSchemaProto,:server.rfi.server.adselect.AdMessagesProto,:common.message.protobuf.userprofile.UserProfileSchemaProto,:server.exchanges.openx.OpenxApiProto,:grid.datascrub.schema.ActionsRecordJavaProto,:modeling.perseus.schema.RtbidsCountJavaProto,:grid.scrubplus.logformat.generated.hive_proto.EvrColumnsProto,:mobile.geo.user.GeoProfileProto,:modeling.perseus.schema.CreditedConversionsJavaProto,:modeling.iq.schema.RtbidsJavaProto,:common.message.protobuf.common.RFIStructuresProto,:grid.onlinestore.model.protobuf.ProtobufRandomLibs,:common.message.protobuf.common.AGStructuresProto,:common.message.protobuf.DatetimeProto,:common.message.protobuf.common.MediaTypeProto,:common.message.protobuf.common.CurrencyTypeProto,:grid.onlinestore.model.protobuf.ProtobufClusterLibs,:luke.LookupJavaProtos,:modeling.perseus.schema.ViewabilityMeasuresForImpressionsProto,:common.message.protobuf.common.DeviceIdStructProto,:brand.insights.protobuf.TvProto,:common.message.protobuf.reporting.DMPSegmentProto,:modeling.perseus.schema.BidwinsJavaProto,:luke.PageJavaProtos,:common.message.protobuf.ApolloProto,:grid.common.hive.utils.HiveProtobufCookieUtils,:grid.datascrub.schema.BotsRecordJavaProto,:grid.datascrub.schema.RemainderRecordJavaProto,:common.message.protobuf.common.SegmentStructuresProto,:common.message.protobuf.common.BTProfileStructuresProto,:common.message.protobuf.common.AdEventStructuresProto,:modeling.perseus.schema.CustomDataTypesJavaProto,:grid.datascrub.schema.MasterAdLogRecordJavaProto,:server.ServerProtoAll,:grid.scrubplus.logformat.generated.hive_proto.DataScrubStatusColumnsProto,:rpcutils.DuplexProtocolJavaProto,:modeling.perseus.schema.ClusterJavaProto,:common.message.protobuf.PageContextProto,:modeling.perseus.schema.BrandProto,:common.message.protobuf.common.EnumStructProto,:common.message.protobuf.UrlHitCountProto,:grid.datascrub.schema.CookieMatchingRecordJavaProto,:modeling.perseus.schema.RtbidsJavaProto,:grid.scrubplus.logformat.generated.proto_scala.GeneratedProtoScalaLib,:grid.onlinestore.model.protobuf.ProtobufModelLibs,:common.message.protobuf.ModelingDataMessageProto,:common.message.protobuf.BidDataMessageProto,:common.message.protobuf.CookieGroupProto,:modeling.perseus.schema.ClicksJavaProto,:modeling.perseus.schema.ImpressionsJavaProto,:server.exchanges.openx.SsrtbProto,:server.rfi.bidder.adx.SnippetStatusReportProto,:grid.scrubplus.logformat.generated.hive_proto.EvfColumnsProto,:modeling.perseus.schema.GeoAudienceSegmentsJavaProto,:modeling.perseus.schema.ActionsJavaProto,:grid.datascrub.schema.CookiesRecordJavaProto,:modeling.perseus.schema.ThirdPartyS2SDataJavaProto,:grid.datascrub.schema.ClicksRecordJavaProto,:common.message.protobuf.AerospikeDataMessageProto,:com.googlecode.protobuf.pro.duplex.DuplexProtobufAll,:common.message.protobuf.common.LocationStructuresProto,:modeling.perseus.schema.AttributionsJavaProto,:common.message.protobuf.common.DMPProfileStructuresProto,:com.googlecode.protobuf.pro.duplex.DuplexLogProto,:common.message.protobuf.TimeAndCountProto,:common.message.protobuf.AdPricesMessageProto,:common.message.protobuf.siteprofile.SiteProfileSchemaProto,:common.message.protobuf.BidKeyDataMessageProto,:common.message.protobuf.artemis.ArtemisSchemaProto,:common.message.protobuf.RTBidProto,:modeling.perseus.schema.ViewsJavaProto,:grid.datascrub.schema.InteractionTrackerRecordJavaProto,:common.message.protobuf.ModelingScoreMessageProto,:grid.scrubplus.logformat.generated.hive_proto.ScrubplusProtobufLib,:server.exchanges.brl.BrlProto,:common.message.protobuf.common.CommonProtobufAll,:common.message.protobuf.common.UserStructuresProto,:common.message.protobuf.TacticDeliveryStatsProto,:common.message.protobuf.ProtobufAll,:common.message.protobuf.AdLogProto,:grid.datascrub.schema.RtbVideoRecordJavaProto,:common.message.protobuf.common.PrimitiveStructuresProto,:server.rfi.server.adselect.MobilePayloadProto,:camus.etl.mapred.support.TestSchemas,:grid.datascrub.schema.TestETLRecordJavaProto,:grid.datascrub.schema.TestETLSubsetRecordJavaProto,:server.util.actions.proto.TestProtobufActionsLib,:rpcutils.RpcTestJavaProto install
```

```
mvn -pl :common.message.thrift.OnlineStoreService thrift:compile
```

When that completes, refresh the Maven project in IntelliJ. IntelliJ will then find the generated Java sources.
