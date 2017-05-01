-- INSERT INTO mvn.dependency_supplements (source_id, group_id, artifact_id, version, scope)
-- SELECT id, 'com.rocketfuel.modeling', 'athena.testdata', 'M1', 'compile'
-- FROM mool.blds
-- WHERE path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsTest']

INSERT INTO mvn.dependency_supplements (source_id, group_id, artifact_id, version, scope, classifier)

SELECT id, 'org.scala-lang', 'scala-actors-migration_2.10', '1.0.0', 'compile', NULL
FROM mool.blds
WHERE path = array['java', 'com', 'rocketfuel', 'grid', 'tools', 'orcinsights', 'RtbidsSizeTracker']

UNION

SELECT id, 'commons-cli', 'commons-cli', '1.3.1', 'test', NULL
FROM mool.blds
WHERE path = array['java', 'com', 'rocketfuel', 'grid', 'onlinestore', 'utils', 'cleanup', 'TestBase']

UNION

SELECT id, 'com.rocketfuel.grid.thirdparty.hbase', 'hbase-server', '1.1.4.262', 'compile', 'test'
FROM mool.blds
WHERE path = array['java', 'com', 'rocketfuel', 'grid', 'common', 'hbase', 'testutils', 'TestHBase']
