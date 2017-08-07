/*
{java,com,rocketfuel,grid,lookup,dim,DimLib}
{java,com,rocketfuel,grid,lookup,dim,DimLibOld}

Create DimCommon.
Add common sources and dependencies to DimLibCommon, remove from DimLib and DimLibOld.
DimLibCommon and DimLibOld depend on DimCommon.
*/

SELECT mool_dedup.factor(
  array['java','com','rocketfuel','grid','lookup','dim','DimLib'],
  array['java','com','rocketfuel','grid','lookup','dim','DimLibOld'],
  array['java','com','rocketfuel','grid','lookup','dim','DimLibCommon']
);

/*
{java,com,rocketfuel,modeling,athena,core,common,RetargetingCommon}
{java,com,rocketfuel,modeling,athena,core,common,Common}
{java,com,rocketfuel,modeling,athena,core,common,RequestDataExtractor}
{java,com,rocketfuel,modeling,athena,core,common,JsonConverter}
{java,com,rocketfuel,modeling,athena,core,common,TrainingDataExtractor}

1. Factor RetargetingCommon into Common.

2. Remove Configuration.scala from Common.

3a. Remove JsonConfiguration.scala from Common.
3b. Add dependency from Common to JsonConverter.
--NOTE: 3b adds dependency to Configuration.

Everything that depends on RequestDataExtractor already depends on Common.
4. Copy sources and dependencies from RequestDataExtractor to Common.

Everything that depends on TrainingDataExtractor already depends on Common.
5. Copy sources and dependencies from TrainingDataExtractor to Common.

6a. Remove TrainingInstanceDataExtractor.scala from RetargetingCommon, TrainingDataExtractor.
6b. Add TrainingInstanceDataExtractor.scala to Common.
*/

--1
SELECT mool_dedup.move_into(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'RetargetingCommon'],
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common']
);

--2
SELECT mool_dedup.remove_source(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'java/com/rocketfuel/modeling/athena/core/common/Configuration.scala'
);

--3a
SELECT mool_dedup.remove_source(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'java/com/rocketfuel/modeling/athena/core/common/JsonConfiguration.scala'
);

--3b
SELECT mool_dedup.add_dependency(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'JsonConverter'],
  false,
  false
);

--4
SELECT mool_dedup.move_into(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'RequestDataExtractor'],
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common']
);

--5
SELECT mool_dedup.move_into(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'TrainingDataExtractor'],
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common']
);

--6
SELECT mool_dedup.add_source(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
  'java/com/rocketfuel/modeling/athena/core/common/TrainingInstanceDataExtractor.scala'
);

/*
{java,com,rocketfuel,modeling,athena,core,common,RetargetingAdScoringInfo}
{java,com,rocketfuel,modeling,athena,core,common,AdScoringInfo}

Replace dependencies on RetargetingAdScoringInfo with AdScoringInfo and Common.
Remove source mappings to RetargetingAdScoringInfo.
Remove BLD RetargetingAdScoringInfo.
*/

SELECT mool_dedup.remove_source(blds.id, source_id)
FROM mool.blds
  INNER JOIN mool.bld_to_sources
    ON blds.id = bld_id
WHERE blds.path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'RetargetingAdScoringInfo'];

INSERT INTO mool_dedup.bld_to_bld_removals (bld_to_bld_id)
  SELECT bld_to_bld.id
  FROM mool.blds targets
    INNER JOIN mool.bld_to_bld
      ON target_id = targets.id
  WHERE targets.path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'RetargetingAdScoringInfo'];

INSERT INTO mool_dedup.bld_to_bld_additions (source_id, target_id, is_compile, is_extract)
  WITH new_targets AS (
    SELECT id
    FROM mool.blds
    WHERE path IN (
      array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'AdScoringInfo'],
      array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common']
    )
  )
  SELECT sources.id, new_targets.id, bld_to_bld.is_compile, bld_to_bld.is_extract
  FROM mool.blds sources
    INNER JOIN mool.bld_to_bld
      ON sources.id = bld_to_bld.source_id
    INNER JOIN mool.blds targets
      ON targets.id = bld_to_bld.target_id
    CROSS JOIN new_targets
  WHERE targets.path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'RetargetingAdScoringInfo'];

INSERT INTO mool_dedup.bld_removals (bld_id)
  SELECT id
  FROM mool.blds
  WHERE path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'RetargetingAdScoringInfo'];

/*
{java,com,rocketfuel,modeling,athena,core,utils,RetargetingUtils}
{java,com,rocketfuel,modeling,athena,core,utils,Utils}

Remove sources and dependencies in Utils that are in RetargetingUtils, unless they are compile.

Make Utils depend on RetargetingUtils
*/

wITH utils_id AS (
  SELECT id
  FROM mool.blds
  WHERE path = ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'Utils']
), sources AS (
  SELECT bld_to_sources.source_id
  FROM mool.bld_to_sources
  WHERE bld_id IN (SELECT id FROM utils_id)
  INTERSECT
  SELECT bld_to_sources.source_id
  FROM mool.blds
    INNER JOIN mool.bld_to_sources
      ON blds.id = bld_to_sources.bld_id
  WHERE blds.path = ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'RetargetingUtils']
)
SELECT mool_dedup.remove_source(utils_id.id, sources.source_id)
FROM utils_id
  CROSS JOIN sources;

wITH utils_id AS (
  SELECT id
  FROM mool.blds
  WHERE path = ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'Utils']
), dependencies AS (
  SELECT bld_to_bld.target_id
  FROM mool.bld_to_bld
  WHERE source_id IN (SELECT id FROM utils_id)
    AND NOT is_compile
  INTERSECT
  SELECT bld_to_bld.target_id
  FROM mool.blds
    INNER JOIN mool.bld_to_bld
      ON blds.id = bld_to_bld.source_id
  WHERE blds.path = ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'RetargetingUtils']
    AND NOT is_compile
)
SELECT mool_dedup.remove_dependency(id, dependencies.target_id)
FROM utils_id
  CROSS JOIN dependencies;

SELECT mool_dedup.add_dependency(
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'Utils'],
  ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'RetargetingUtils'],
  false,
  false
);

/*
{java,com,rocketfuel,server,util,ArrayUtils}
{java,com,rocketfuel,server,util,SupportLib}

Remove ArrayUtils.java from SupportLib.
Make SupportLib depend on ArrayUtils.
*/

SELECT mool_dedup.remove_source(
  array['java', 'com', 'rocketfuel', 'server', 'util', 'SupportLib'],
  'java/com/rocketfuel/server/util/ArrayUtils.java'
);

SELECT mool_dedup.add_dependency(
  array['java', 'com', 'rocketfuel', 'server', 'util', 'SupportLib'],
  array['java', 'com', 'rocketfuel', 'server', 'util', 'ArrayUtils'],
  false,
  false
);

/*
{java,com,rocketfuel,grid,common,db,Databases}
{java,com,rocketfuel,grid,common,db,Databases210}

This should be cross compiled, which I'm not sure is possible with Maven.
Idea: create a project with two profiles. One profile has name suffix, _2.10, other has _2.11.
*/

/*
{java,com,rocketfuel,modeling,athena,core,modules,RetargetingModules}
{java,com,rocketfuel,modeling,athena,core,modules,Modules}

Move RetargetingModules into ModulesCommon.
*/

SELECT mool_dedup.move_into(
  array['java','com','rocketfuel','modeling','athena','core','modules','RetargetingModules'],
  array['java','com','rocketfuel','modeling','athena','core','modules','Modules']
);

/*
{java,com,rocketfuel,modeling,common,JavaSource}
{java,com,rocketfuel,modeling,common,BidRequestScoringContext}
{java,com,rocketfuel,modeling,common,FeatureScore}
{java,com,rocketfuel,modeling,common,BtFeatureType}
{java,com,rocketfuel,modeling,common,CampaignGrouping}
{java,com,rocketfuel,modeling,common,ConversionActionType}
{java,com,rocketfuel,modeling,common,FeatureScore}
{java,com,rocketfuel,modeling,common,DependenceLevel}
{java,com,rocketfuel,modeling,common,DoubleValueContainer}
{java,com,rocketfuel,modeling,common,FeatureGroupKey}
{java,com,rocketfuel,modeling,common,FeatureScore}
{java,com,rocketfuel,modeling,common,FeatureScore}
{java,com,rocketfuel,modeling,common,FeatureScore}
{java,com,rocketfuel,modeling,common,FeatureTierType}
{java,com,rocketfuel,modeling,common,FoldCode}
{java,com,rocketfuel,modeling,common,ModelingConstants}
{java,com,rocketfuel,modeling,common,PredictionType}
{java,com,rocketfuel,modeling,common,TierMapUtils}
{java,com,rocketfuel,modeling,common,FeatureScore}

Remove the following sources from JavaSource, add dependencies from JavaSource to the above.

java/com/rocketfuel/modeling/common/BidRequestScoringContext.java
java/com/rocketfuel/modeling/common/BrandAgScore.java
java/com/rocketfuel/modeling/common/BtFeatureType.java
java/com/rocketfuel/modeling/common/CampaignGrouping.java
java/com/rocketfuel/modeling/common/ConversionActionType.java
java/com/rocketfuel/modeling/common/CtrScore.java
java/com/rocketfuel/modeling/common/DependenceLevel.java
java/com/rocketfuel/modeling/common/DoubleValueContainer.java
java/com/rocketfuel/modeling/common/FeatureGroupKey.java
java/com/rocketfuel/modeling/common/FeatureScore.java
java/com/rocketfuel/modeling/common/FeatureScoreType.java
java/com/rocketfuel/modeling/common/FeatureScoreUtils.java
java/com/rocketfuel/modeling/common/FeatureTierType.java
java/com/rocketfuel/modeling/common/FoldCode.java
java/com/rocketfuel/modeling/common/ModelingConstants.java
java/com/rocketfuel/modeling/common/PredictionType.java
java/com/rocketfuel/modeling/common/TierMapUtils.java
java/com/rocketfuel/modeling/common/UpliftScore.java
*/

SELECT mool_dedup.remove_source(array['java','com','rocketfuel','modeling','common','JavaSource'], path)
FROM (
       VALUES
         ('java/com/rocketfuel/modeling/common/BidRequestScoringContext.java'),
         ('java/com/rocketfuel/modeling/common/BrandAgScore.java'),
         ('java/com/rocketfuel/modeling/common/BtFeatureType.java'),
         ('java/com/rocketfuel/modeling/common/CampaignGrouping.java'),
         ('java/com/rocketfuel/modeling/common/ConversionActionType.java'),
         ('java/com/rocketfuel/modeling/common/CtrScore.java'),
         ('java/com/rocketfuel/modeling/common/DependenceLevel.java'),
         ('java/com/rocketfuel/modeling/common/DoubleValueContainer.java'),
         ('java/com/rocketfuel/modeling/common/FeatureGroupKey.java'),
         ('java/com/rocketfuel/modeling/common/FeatureScore.java'),
         ('java/com/rocketfuel/modeling/common/FeatureScoreType.java'),
         ('java/com/rocketfuel/modeling/common/FeatureScoreUtils.java'),
         ('java/com/rocketfuel/modeling/common/FeatureTierType.java'),
         ('java/com/rocketfuel/modeling/common/FoldCode.java'),
         ('java/com/rocketfuel/modeling/common/ModelingConstants.java'),
         ('java/com/rocketfuel/modeling/common/PredictionType.java'),
         ('java/com/rocketfuel/modeling/common/TierMapUtils.java'),
         ('java/com/rocketfuel/modeling/common/UpliftScore.java')
     ) AS source(path);

SELECT mool_dedup.add_dependency(array['java','com','rocketfuel','modeling','common','JavaSource'], path, false, false)
FROM (
       VALUES
         (array['java','com','rocketfuel','modeling','common','BidRequestScoringContext']),
         (array['java','com','rocketfuel','modeling','common','FeatureScore']),
         (array['java','com','rocketfuel','modeling','common','BtFeatureType']),
         (array['java','com','rocketfuel','modeling','common','CampaignGrouping']),
         (array['java','com','rocketfuel','modeling','common','ConversionActionType']),
         (array['java','com','rocketfuel','modeling','common','FeatureScore']),
         (array['java','com','rocketfuel','modeling','common','DependenceLevel']),
         (array['java','com','rocketfuel','modeling','common','DoubleValueContainer']),
         (array['java','com','rocketfuel','modeling','common','FeatureGroupKey']),
         (array['java','com','rocketfuel','modeling','common','FeatureScore']),
         (array['java','com','rocketfuel','modeling','common','FeatureScore']),
         (array['java','com','rocketfuel','modeling','common','FeatureScore']),
         (array['java','com','rocketfuel','modeling','common','FeatureTierType']),
         (array['java','com','rocketfuel','modeling','common','FoldCode']),
         (array['java','com','rocketfuel','modeling','common','ModelingConstants']),
         (array['java','com','rocketfuel','modeling','common','PredictionType']),
         (array['java','com','rocketfuel','modeling','common','TierMapUtils']),
         (array['java','com','rocketfuel','modeling','common','FeatureScore'])

     ) AS target(path);

/*
{java,com,rocketfuel,ei,datamon,alert,AlertUtilityTest}
{java,com,rocketfuel,ei,datamon,alert,AlertUtilityTestLib}

Add sources in AlertUtilityTestLib to AlertUtilityTest. Remove AlertUtilityTestLib.
*/

SELECT mool_dedup.move_source(
  array['java','com','rocketfuel','ei','datamon','alert','AlertUtilityTest'],
  array['java','com','rocketfuel','ei','datamon','alert','AlertUtilityTestLib'],
  sources.path
)
FROM mool.blds
  INNER JOIN mool.bld_to_sources
    ON blds.id = bld_to_sources.bld_id
  INNER JOIN mool.sources
    ON bld_to_sources.source_id = sources.id
WHERE blds.path = array['java','com','rocketfuel','ei','datamon','alert','AlertUtilityTest'];

/*
{java,com,rocketfuel,modeling,athena,pipelines,conversion,longRun,Modules}
{java,com,rocketfuel,modeling,athena,pipelines,conversion,longRun,LongRunConversion}
{java,com,rocketfuel,modeling,athena,pipelines,conversion,longRun,LongRunConversionsCampaignSelector}

Remove LongRunConversionsCampaignSelector and Modules. Dependencies on LongRunConversionsCampaignSelector or Modules point to LongRunConversion.
*/

INSERT INTO mool_dedup.bld_to_bld_additions (source_id, target_id, is_compile, is_extract)
  SELECT source.id, new_target.id, bld_to_bld.is_compile, bld_to_bld.is_extract
  FROM mool.bld_to_bld
    INNER JOIN mool.blds source
      ON source.id = bld_to_bld.source_id
    INNER JOIN mool.blds target
      ON target.id = bld_to_bld.target_id
         AND target.path IN (
      array['java','com','rocketfuel','modeling','athena','pipelines','conversion','longRun','Modules'],
      array['java','com','rocketfuel','modeling','athena','pipelines','conversion','longRun','LongRunConversionsCampaignSelector']
    )
    CROSS JOIN (
                 SELECT *
                 FROM mool.blds
                 WHERE path = array['java','com','rocketfuel','modeling','athena','pipelines','conversion','longRun','LongRunConversion']
               ) new_target;

SELECT mool_dedup.remove_bld(array['java','com','rocketfuel','modeling','athena','pipelines','conversion','longRun','Modules']);
SELECT mool_dedup.remove_bld(array['java','com','rocketfuel','modeling','athena','pipelines','conversion','longRun','LongRunConversionsCampaignSelector']);

/*
{java,com,rocketfuel,ei,common,SimpleReconnectingRpcClientTest}
{java,com,rocketfuel,ei,common,ListBasedReconnectingRpcClientTest}

Add RpcTestUtils.java to RpcTest.
Remove RpcTestUtils.java from SimpleReconnectingRpcClientTest, ListBasedReconnectingRpcClientTest.
*/

INSERT INTO mool_dedup.bld_to_source_additions (bld_id, source_id)
  SELECT blds.id, sources.id
  FROM mool.blds
    CROSS JOIN mool.sources
  WHERE blds.path = array['java','com','rocketfuel','ei','common','RpcTest']
        AND sources.path = 'java/com/rocketfuel/ei/common/RpcTestUtils.java';

SELECT mool_dedup.remove_source(array['java','com','rocketfuel','ei','common','ListBasedReconnectingRpcClientTest'], 'java/com/rocketfuel/ei/common/RpcTestUtils.java');

SELECT mool_dedup.remove_source(array['java','com','rocketfuel','ei','common','SimpleReconnectingRpcClientTest'], 'java/com/rocketfuel/ei/common/RpcTestUtils.java');

/*
{java,com,rocketfuel,modeling,athena,auxiliarytask,RetargetingAuxiliaryTask}
{java,com,rocketfuel,modeling,athena,auxiliarytask,AuxiliaryTask}

Create AuxiliaryTaskCommon, with sources and dependencies in common between RetargetingAuxiliaryTask and AuxiliaryTask.

Remove sources and dependencies in common from RetargetingAuxiliaryTask and AuxiliaryTask.

RetargetingAuxiliaryTask and AuxiliaryTask depend on AuxiliaryTaskCommon.
*/

SELECT mool_dedup.factor(
  array['java','com','rocketfuel','modeling','athena','auxiliarytask','RetargetingAuxiliaryTask'],
  array['java','com','rocketfuel','modeling','athena','auxiliarytask','AuxiliaryTask'],
  array['java','com','rocketfuel','modeling','athena','auxiliarytask','AuxiliaryTaskCommon']
);

/*
{java,com,rocketfuel,modeling,athena,core,modules,Modules}
{java,com,rocketfuel,modeling,athena,core,modules,DefaultUserSelector}
{java,com,rocketfuel,modeling,athena,core,modules,BtModules}

Remove sources from Modules:
java/com/rocketfuel/modeling/athena/core/modules/BTProfileDecoderModule.scala
java/com/rocketfuel/modeling/athena/core/modules/BTProfileReaderModule.scala
java/com/rocketfuel/modeling/athena/core/modules/BTUserProfileJoinerModule.scala

Add dependency from Modules to BtModules.

Remove DefaultUserSelector.
Replace dependencies on DefaultUserSelector with Modules.
*/

INSERT INTO mool_dedup.bld_to_source_removals (bld_to_source_id)
  SELECT bld_to_sources.id
  FROM mool.blds
    INNER JOIN mool.bld_to_sources
      ON blds.id = bld_to_sources.bld_id
    INNER JOIN mool.sources
      ON bld_to_sources.source_id = sources.id
         AND sources.path in (
      'java/com/rocketfuel/modeling/athena/core/modules/BTProfileDecoderModule.scala',
      'java/com/rocketfuel/modeling/athena/core/modules/BTProfileReaderModule.scala',
      'java/com/rocketfuel/modeling/athena/core/modules/BTUserProfileJoinerModule.scala'
    )
  WHERE blds.path = array['java','com','rocketfuel','modeling','athena','core','modules','Modules'];

SELECT mool_dedup.add_dependency(
  array['java','com','rocketfuel','modeling','athena','core','modules','Modules'],
  array['java','com','rocketfuel','modeling','athena','core','modules','BtModules'],
  false,
  false
);

INSERT INTO mool_dedup.bld_to_bld_additions (source_id, target_id, is_compile, is_extract)
SELECT bld_to_bld.source_id, (SELECT id FROM mool.blds WHERE path = array['java','com','rocketfuel','modeling','athena','core','modules','Modules']), bld_to_bld.is_compile, bld_to_bld.is_extract
FROM mool.bld_to_bld
WHERE bld_to_bld.target_id = (SELECT id FROM mool.blds WHERE path = array['java','com','rocketfuel','modeling','athena','core','modules','DefaultUserSelector']);

INSERT INTO mool_dedup.bld_to_bld_removals (bld_to_bld_id)
  SELECT bld_to_bld.id
  FROM mool.bld_to_bld
  WHERE target_id = (SELECT id FROM mool.blds WHERE path = array['java','com','rocketfuel','modeling','athena','core','modules','DefaultUserSelector']);

/*
{java,com,rocketfuel,modeling,athena,core,workflow,WorkFlow}
{java,com,rocketfuel,modeling,athena,core,workflow,RetargetingWorkFlow}

Remove sources from Workflow:
java/com/rocketfuel/modeling/athena/core/workflow/PipelineTemplate.scala
java/com/rocketfuel/modeling/athena/core/workflow/CheckPointApi.scala

Add dependency from WorkFlow to RetargetingWorkFlow.

Remove common BLD dependencies from WorkFlow.
*/

SELECT mool_dedup.remove_source(
  array['java','com','rocketfuel','modeling','athena','core','workflow','WorkFlow'],
  'java/com/rocketfuel/modeling/athena/core/workflow/PipelineTemplate.scala'
);

SELECT mool_dedup.remove_source(
  array['java','com','rocketfuel','modeling','athena','core','workflow','WorkFlow'],
  'java/com/rocketfuel/modeling/athena/core/workflow/CheckPointApi.scala'
);

SELECT mool_dedup.add_dependency(
  array['java','com','rocketfuel','modeling','athena','core','workflow','WorkFlow'],
  array['java','com','rocketfuel','modeling','athena','core','workflow','RetargetingWorkFlow'],
  false,
  false
);

WITH targets_of_retargeting AS (
  SELECT targets.path
  FROM mool.blds sources
    INNER JOIN mool.bld_to_bld
      ON bld_to_bld.source_id = sources.id
    INNER JOIN mool.blds targets
      ON bld_to_bld.target_id = targets.id
  WHERE sources.path = ARRAY ['java', 'com,', 'rocketfuel', 'modeling', 'athena', 'core', 'workflow', 'RetargetingWorkFlow']
), targets_of_workflow AS (
  SELECT targets.path
  FROM mool.blds sources
    INNER JOIN mool.bld_to_bld
      ON bld_to_bld.source_id = sources.id
    INNER JOIN mool.blds targets
      ON bld_to_bld.target_id = targets.id
  WHERE sources.path = ARRAY ['java', 'com,', 'rocketfuel', 'modeling', 'athena', 'core', 'workflow', 'WorkFlow']
)
SELECT mool_dedup.remove_dependency(array['java','com,rocketfuel','modeling','athena','core','workflow','WorkFlow'], common_targets.path)
FROM (
       SELECT *
       FROM targets_of_retargeting
       INTERSECT
       SELECT *
       FROM targets_of_workflow
     ) common_targets (path);


/*
{java,com,rocketfuel,grid,dmp,ssvadapter,ActSegmentAdapterCols}
{java,com,rocketfuel,grid,dmp,ssvadapter,BidRequestAdapterCols}

Add dependency from ActSegmentAdapterCols to BidRequestAdapterCols.
Remove BidRequestInputCols.java from ActSegmentAdapterCols.
*/

SELECT mool_dedup.add_dependency(
  array['java','com','rocketfuel','grid','dmp','ssvadapter','ActSegmentAdapterCols'],
  array['java','com','rocketfuel','grid','dmp','ssvadapter','BidRequestAdapterCols'],
  false,
  false
);

SELECT mool_dedup.remove_source(
  array['java','com','rocketfuel','grid','dmp','ssvadapter','ActSegmentAdapterCols'],
  'java/com/rocketfuel/grid/dmp/ssvadapter/BidRequestInputCols.java'
);

/*
{java,com,rocketfuel,server,util,SupportLib}
{java,com,rocketfuel,server,util,CollectionUtils}
{java,com,rocketfuel,server,util,StringHashFunction}

Add dependency from SupportLib to CollectionUtils, StringHashFunction.

Remove
java/com/rocketfuel/server/util/CollectionUtils.java
java/com/rocketfuel/server/util/StringHashFunction.java
from SupportLib.
*/

SELECT mool_dedup.add_dependency(
  array['java','com','rocketfuel','server','util','SupportLib'],
  array['java','com','rocketfuel','server', 'util', 'CollectionUtils'],
  false,
  false
);

SELECT mool_dedup.add_dependency(
  array['java','com','rocketfuel','server','util','SupportLib'],
  array['java','com','rocketfuel','server','util','StringHashFunction'],
  false,
  false
);

SELECT mool_dedup.remove_source(
  array['java','com','rocketfuel','server','util','SupportLib'],
  'java/com/rocketfuel/server/util/CollectionUtils.java'
);

SELECT mool_dedup.remove_source(
  array['java','com','rocketfuel','server','util','SupportLib'],
  'java/com/rocketfuel/server/util/StringHashFunction.java'
);

/*
{java,com,rocketfuel,modeling,athena,core,common,TrainingInstanceTest}
{java,com,rocketfuel,modeling,athena,core,common,CommonTest}

factor
*/

SELECT mool_dedup.factor(
  array['java','com','rocketfuel','modeling','athena','core','common','TrainingInstanceTest'],
  array['java','com','rocketfuel','modeling','athena','core','common','CommonTest'],
  array['java','com','rocketfuel','modeling','athena','core','common','TestCommon']
);

/*
{java,com,rocketfuel,grid,luke,service,client,LookupCacheTest}
{java,com,rocketfuel,grid,luke,service,client,TtlLookupCacheHeavyTest}

Delete TtlLookupCacheHeavyTest.
*/

SELECT mool_dedup.remove_bld(array['java','com','rocketfuel','grid','luke','service','client','TtlLookupCacheHeavyTest']);

/*
Add dependency from ['java', 'com', 'rocketfuel', 'grid', 'onlinestore', 'offlineupload', 'Base'] to commons-cli.
 */

SELECT mool_dedup.add_dependency(
  array['java', 'com', 'rocketfuel', 'grid', 'onlinestore', 'offlineupload', 'Base'],
  array['java', 'mvn', 'commons-cli', 'CommandLineInterface'],
  false,
  false
);

--Avoid ClassNotFoundException during testing.
SELECT mool_dedup.add_dependency(
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'ml', 'models', 'ModelsTest'],
  array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'ml', 'models', 'AthenaLogisticRegressionModel'],
  true,
  false
);

--Avoid wrong version of AdScoringInfo.
SELECT mool_dedup.remove_dependency(
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'AdScoringInfo'],
  ARRAY['java', 'mvn', 'com', 'rocketfuel', 'grid', 'modeling', 'GridModeling']
);

SELECT mool_dedup.add_dependency(
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'AdScoringInfo'],
  ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'ModelingConstants'],
  false,
  false
);

