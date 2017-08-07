--this is just wrong
UPDATE mool.blds
SET artifact_id = 'json4s-native_2.10'
WHERE artifact_id = 'json4s-native_2.9.2';

--Some Scala 2.11 projects rely on json4s for Scala 2.10. This is just wrong.
--This is a three step fix.
--Step 1: create dependencies for json4s_2.11.
INSERT INTO mool.blds (path, rule_type, group_id, artifact_id, version)
SELECT
  array_append(path, '2.11'),
  rule_type,
  group_id,
  substring(artifact_id from E'.*_2\\.1') || '1',
  version
FROM mool.blds
WHERE group_id = 'org.json4s';

--Step 2: Insert dependencies for json4s_2.11 where a scala 2.10 project depends on json4s_2.10.
INSERT INTO mool.bld_to_bld (source_id, target_id, is_compile, is_extract)
SELECT source.id, json4s.id, false, false --compile just happens to be always false for json4s
FROM mool.blds source
  INNER JOIN mool.bld_to_bld
    ON source.id = bld_to_bld.source_id
  INNER JOIN mool.blds target
    ON bld_to_bld.target_id = target.id
       AND target.path = ARRAY['java', 'mvn', 'org', 'json4s', 'Json4sAll']
CROSS JOIN mool.blds json4s
WHERE
  source.scala_version = '2.11'
  AND json4s.group_id = 'org.json4s'
  AND json4s.artifact_id LIKE '%_2.11';

--Step 3: Scala 2.11 projects should depend on Json4sAll2_11, not Json4sAll.

UPDATE mool.bld_to_bld
SET target_id = (SELECT id FROM mool.blds WHERE path = ARRAY['java', 'mvn', 'org', 'json4s', 'Json4sAll2_11'])
FROM mool.blds
WHERE
  blds.scala_version = '2.11'
  AND source_id = blds.id
  AND target_id = (SELECT id FROM mool.blds WHERE path = ARRAY['java', 'mvn', 'org', 'json4s', 'Json4sAll']);

/*
Use a newer version of json4s, or else we sometimes get

java.lang.NoSuchMethodError: com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder.addField(Lcom/fasterxml/jackson/databind/introspect/AnnotatedField;Lcom/fasterxml/jackson/databind/PropertyName;ZZZ)V

or

Cause: java.lang.NoSuchMethodError: com.fasterxml.jackson.core.JsonFactory.requiresPropertyOrdering()Z

EncodingModulesTest, DefaultPrepareEncodedTest

Json4s 3.3.0 uses jackson 2.6.2, which almost matches RetargetingUtils's use of 2.6.5.
 */

-- UPDATE mool.blds
-- SET version = '3.3.0'
-- WHERE group_id = 'org.json4s';

--The BLD's version brings in a 2 year old version of a class, causing compiling to fail
--for com.rocketfuel.grid:reporting.revenue_capping.driver_lib and others.
-- UPDATE mool.blds
-- SET version = '100.24.17'
-- WHERE group_id = 'com.rocketfuel.grid.hiveudf'
--   AND artifact_id = 'grid.hiveudf';

--reverse "# Hack as empty sources not allowed."
DELETE FROM mool.bld_to_sources
USING mool.blds, mool.sources
WHERE bld_to_sources.bld_id = blds.id
  AND blds.path = regexp_split_to_array('java/com/rocketfuel/grid/onlinestore/cluster/TestClusteredOnlineStore', '/')
  AND sources.path = 'java/com/rocketfuel/grid/onlinestore/cluster/ClusterType.java';


--Scala projects that rely on ScalaLibrary should rely on ScalaLibrary2_11.

UPDATE mool.bld_to_bld
SET target_id = (SELECT id FROM mool.blds WHERE path = ARRAY['java', 'mvn', 'org', 'scala-lang', 'ScalaLibrary2_11'])
FROM mool.blds
WHERE
  blds.scala_version = '2.11'
  AND source_id = blds.id
  AND target_id = (SELECT id FROM mool.blds WHERE path = ARRAY['java', 'mvn', 'org', 'scala-lang', 'ScalaLibrary']);
