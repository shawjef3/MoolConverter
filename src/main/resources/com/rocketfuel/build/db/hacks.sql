--this is just wrong
UPDATE mool.blds
SET artifact_id = 'json4s-native_2.10'
WHERE artifact_id = 'json4s-native_2.9.2';

--this is just wrong
UPDATE mool.blds
SET artifact_id = 'scalatest_2.10'
WHERE artifact_id = 'scalatest_2.11';

--Depending on the original brings in a 2 year old version of a class, causing compiling to fail
--for com.rocketfuel.grid:reporting.revenue_capping.driver_lib.
UPDATE mool.blds
SET version = '100.24.17'
WHERE group_id = 'com.rocketfuel.grid.hiveudf'
  AND artifact_id = 'grid.hiveudf';

--reverse "# Hack as empty sources not allowed."
DELETE FROM mool.bld_to_sources
USING mool.blds, mool.sources
WHERE bld_to_sources.bld_id = blds.id
  AND blds.path = regexp_split_to_array('java/com/rocketfuel/grid/onlinestore/cluster/TestClusteredOnlineStore', '/')
  AND sources.path = 'java/com/rocketfuel/grid/onlinestore/cluster/ClusterType.java';

--Use the cloned grid.modeling. Requires Convert.gridModeling(Path).
-- This fixes  the version of AdScoringInfo to be used during compiling.
UPDATE mool.blds
SET version = 'M1'
WHERE group_id = 'com.rocketfuel.grid.modeling'
  AND artifact_id = 'grid.modeling';

--add dependencies for com.rocketfuel.modeling:athena.core.common.CommonTest
WITH target_ids AS (
    SELECT id
    FROM mool.blds target
    WHERE (group_id = 'com.esotericsoftware.kryo'
           AND artifact_id = 'kryo'
          ) OR (
                group_id = 'org.apache.spark'
                AND artifact_id = 'spark-assembly_2.10'
                AND version = '1.6.1'
          ) OR path = ARRAY['java', 'com', 'rocketfuel', 'modeling', 'common', 'JavaSource']
)
INSERT INTO mool.bld_to_bld (source_id, target_id, is_compile)
SELECT source.id, target_ids.id, false
FROM mool.blds source
      CROSS JOIN target_ids
WHERE path = ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'CommonTest']

-- This fixes a wrong version of AdScoringInfo to be used during compiling.
-- This is a class conflict that should be resolved in vostok.
-- The implementation for this fix is more complicated than it needs to be,
-- because I thought the problem was more complicated than it is.


-- CREATE TEMPORARY TABLE exclude_remote_grid_modeling (
--   bld_id int NOT NULL
-- );

-- CREATE TEMPORARY TABLE requires_local_modeling_common (
--   bld_id int NOT NULL
-- );

-- INSERT INTO requires_local_modeling_common (bld_id)
-- SELECT id
-- FROM mool.blds
-- WHERE
--   path[1:7] = ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common']
--   AND path[8] IN (
--     'AdScoringInfo',
--     'AdScoringInfoTest',
--     'RetargetingAdScoringInfo'
--   );

-- INSERT INTO exclude_remote_grid_modeling (bld_id)
--   SELECT id
--   FROM mool.blds
--   WHERE
--     path[1:7] = ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common']
--     AND path[8] IN (
--       'AdScoringInfo',
--       'AdScoringInfoTest',
--       'RetargetingAdScoringInfo'
--     );

-- INSERT INTO mool.bld_to_bld (source_id, target_id, is_compile)
-- SELECT
--   bld_id,
--   (SELECT id FROM mool.blds WHERE path = array['java', 'com', 'rocketfuel', 'modeling', 'common', 'ModelingCommon']),
--   true
-- FROM requires_local_modeling_common;

-- DELETE FROM mool.bld_to_bld
-- USING exclude_remote_grid_modeling
-- WHERE
--   bld_to_bld.source_id IN (
--     SELECT id
--     FROM mool.blds
--     WHERE
--       path[1:7] = ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common']
--       AND path[8] IN (
--         'AdScoringInfo',
--         'AdScoringInfoTest',
--         'RetargetingAdScoringInfo'
--       )
--   ) AND target_id IN (
--     SELECT id
--     FROM mool.blds
--     WHERE group_id = 'com.rocketfuel.grid.modeling'
--           AND artifact_id = 'grid.modeling'
--           AND version = '103.32.0'
--   );

-- --step 1: add ModelingCommon to projects depending on com.rocketfuel.grid.modeling:grid.modeling.
-- INSERT INTO mool.bld_to_bld (source_id, target_id, is_compile)
-- SELECT
--   bld_to_bld.source_id,
--   (SELECT id FROM mool.blds WHERE path = array['java', 'com', 'rocketfuel', 'modeling', 'common', 'ModelingCommon']),
--   is_compile
-- FROM mool.bld_to_bld
-- INNER JOIN mool.blds
--     ON bld_to_bld.target_id = blds.id
--       AND group_id = 'com.rocketfuel.grid.modeling'
--        AND artifact_id = 'grid.modeling'
--        AND version = '103.32.0';
--
-- -- --step 2: Remove dependencies on com.rocketfuel.grid.modeling:grid.modeling.
-- DELETE FROM mool.bld_to_bld
-- USING mool.blds
-- WHERE
--   bld_to_bld.source_id = blds.id
--   AND blds.path <> ARRAY['java', 'com', 'rocketfuel', 'modeling', 'common', 'Common']
--   AND target_id IN (
--     SELECT id
--     FROM mool.blds
--     WHERE group_id = 'com.rocketfuel.grid.modeling'
--           AND artifact_id = 'grid.modeling'
--           AND version = '103.32.0'
--   );
--   AND source_id NOT IN (
--     SELECT id
--     FROM mool.blds
--     WHERE path IN (
--       ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'],
--       ARRAY['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'TrainingDataExtractor']
--     )
--   )
