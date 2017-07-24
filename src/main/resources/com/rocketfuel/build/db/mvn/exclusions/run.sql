SELECT mvn.exclude_servlets(ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common']);
SELECT mvn.exclude_servlets(ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'pipelines', 'bidReqCompare', 'SingleBidReqCompare']);

SELECT mvn.create_exclusion(
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  ARRAY['java', 'mvn', 'com', 'rocketfuel', 'grid', 'modeling', 'GridModeling'],
  'com.rocketfuel.modeling.common',
  'modeling.common'
);


SELECT mvn.create_exclusion(
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'AdScoringInfo'],
  ARRAY['java', 'mvn', 'com', 'rocketfuel', 'grid', 'modeling', 'GridModeling'],
  'com.rocketfuel.modeling.common',
  'modeling.common'
);

--prevent wrong version of AgBucket from being used
SELECT mvn.create_exclusion(
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'Modules'],
  ARRAY['java', 'mvn', 'com', 'rocketfuel', 'grid', 'modeling', 'GridModeling'],
  'com.rocketfuel.server.util',
  'server.util'
);

--prevent wrong version of AgBucket from being used
SELECT mvn.create_exclusion(
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'Modules'],
  ARRAY['java', 'mvn', 'com', 'rocketfuel', 'grid', 'modeling', 'GridModeling'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'EncodingModulesTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'EncodingModulesTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--solve missing class, possibly due to above exclusions.
SELECT mool_dedup.add_dependency(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'EncodingModulesTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'workflow', 'RetargetingWorkFlow'],
  false,
  false
);

--Same exclusions as EncodingModulesTest, for the same reasons.

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'DefaultPrepareEncodedTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'DefaultPrepareEncodedTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsSparkTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsSparkTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--solve missing class, possibly due to above exclusions.
SELECT mool_dedup.add_dependency(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsSparkTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'workflow', 'RetargetingWorkFlow'],
  false,
  false
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsSparkTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'Utils'],
  'com.rocketfuel.java',
  'mvn.org.json4s.Json4sAll'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

--prevent java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'ml', 'models', 'ModelsTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'ml', 'models', 'ModelsTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

--prevent java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'LogisticRegressionOWLQNModuleTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'LogisticRegressionOWLQNModuleTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

--prevent java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'TestHiveContextTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'TestHiveContextTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

--prevent java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'dataframe', 'DataFrameTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'dataframe', 'DataFrameTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

--prevent java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z
SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'pipelines', 'featureDist', 'FeatureDistTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'pipelines', 'featureDist', 'FeatureDist'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

--prevent java.lang.VerifyError: class com.fasterxml.jackson.module.scala.ser.ScalaIteratorSerializer overrides final method withResolved.(Lcom/fasterxml/jackson/databind/BeanProperty;Lcom/fasterxml/jackson/databind/jsontype/TypeSerializer;Lcom/fasterxml/jackson/databind/JsonSerializer;)Lcom/fasterxml/jackson/databind/ser/std/AsArraySerializerBase;

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'pipelines', 'featureDist', 'FeatureDistTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'pipelines', 'featureDist', 'FeatureDist'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);

--Same exclusions as EncodingModulesTest, for the same reasons.

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'viewability', 'ViewabilityUserSelectorModuleTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.rocketfuel.grid.profileupdater',
  'grid.profileupdater'
);

SELECT mvn.create_exclusion(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'modules', 'viewability', 'ViewabilityUserSelectorModuleTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'com.fasterxml.jackson.core',
  'jackson-databind'
);
