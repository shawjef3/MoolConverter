--reverse "# Hack as empty sources not allowed."
DELETE FROM mool.bld_to_sources
USING mool.blds, mool.sources
WHERE bld_to_sources.bld_id = blds.id
  AND blds.path = regexp_split_to_array('java/com/rocketfuel/grid/onlinestore/cluster/TestClusteredOnlineStore', '/')
  AND sources.path = 'java/com/rocketfuel/grid/onlinestore/cluster/ClusterType.java';
