package com.rocketfuel.build.mool

import java.nio.file.Paths

class ModelApp extends App {

  val supplement =
    Map(
      Vector("java","com","rocketfuel","grid","lookup","dim","config","ConfigLib") ->
        Set(
          Vector("java","com","rocketfuel","grid","lookup","dim","config","ConfigurationTest"),
          Vector("java","com","rocketfuel","grid","lookup","dim","config","JSONConfigurationReaderTest"),
          Vector("java","com","rocketfuel","grid","lookup","dim","config","JSONLazyLoadConfigurationReaderTest"),
          Vector("java","com","rocketfuel","grid","lookup","dim","config","ConfigurationTest")
        ),
      Vector("java","com","rocketfuel","modeling","athena","core","common","Common") -> Set(Vector("java","com","rocketfuel","modeling","athena","core","common","BacktestUtilsTest")),
      Vector("java","com","rocketfuel","grid","luke","service","core","lookupservice","LookupService") -> Set(Vector("java","com","rocketfuel","grid","luke","service","core","lookupservice","LookupServiceImplTest")),
      Vector("java","com","rocketfuel","grid","scrubplus","logformat","datatype","DataTypeLib") -> Set(Vector("java","com","rocketfuel","grid","scrubplus","logformat","datatype","DataTypeTests")),
      Vector("java","com","rocketfuel","grid","externalreport","hivereducer","conversion","ConversionReducer") -> Set(Vector("java","com","rocketfuel","grid","externalreport","hivereducer","conversion","AggregateConversionAndAdvertiserDataReducerTest")),
      Vector("java","com","rocketfuel","modeling","bt","offline","data","hbase","HBaseEntities") -> Set(Vector("java","com","rocketfuel","modeling","bt","offline","data","btFeature","BtFeatureTest")),
      Vector("java","com","rocketfuel","mobile","geo","user","segments","ReadGeoProfileMapper") -> Set(Vector("java","com","rocketfuel","mobile","geo","user","segments","GeoProfileHBaseReaderMapperTest")),
      Vector("java","com","rocketfuel","grid","externalreport","data","DataFilters") -> Set(Vector("java","com","rocketfuel","grid","externalreport","data","FilterPublisherDataReducerTest")),
      Vector("java","com","rocketfuel","grid","externalreport","tsdbmetrics","TSDBMetricsPopulator") -> Set(Vector("java","com","rocketfuel","grid","externalreport","tsdbmetrics","AutoloadMetricsDAOTest"))

    )

  val moolRoot = Paths.get(System.getProperty("user.home")).resolve("git/data/vostok")

  val model = Model.ofRepository(moolRoot, supplement)

}
