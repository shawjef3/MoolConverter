--reverse "# Hack as empty sources not allowed."
DELETE FROM mool.bld_to_sources
USING mool.blds, mool.sources
WHERE bld_to_sources.bld_id = blds.id
  AND blds.path = regexp_split_to_array('java/com/rocketfuel/grid/onlinestore/cluster/TestClusteredOnlineStore', '/')
  AND sources.path = 'java/com/rocketfuel/grid/onlinestore/cluster/ClusterType.java';

--This fixes a wrong version of AdScoringInfo to be used during compiling.
--This is a class conflict that should be resolved in vostok.
DELETE FROM mool.bld_to_bld
WHERE
  target_id IN (
    SELECT id
    FROM mool.blds
    WHERE group_id = 'com.rocketfuel.grid.modeling'
          AND artifact_id = 'grid.modeling'
          AND version = '103.32.0'
  )
  AND source_id NOT IN (
    SELECT id
    FROM mool.blds
    WHERE path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common']
  )
