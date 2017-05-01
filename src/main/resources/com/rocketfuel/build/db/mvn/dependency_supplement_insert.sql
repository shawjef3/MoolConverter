INSERT INTO mvn.dependency_supplements (source_id, group_id, artifact_id, version, scope, type)

SELECT id, 'org.scala-lang', 'scala-actors-migration_2.10', '1.0.0', 'compile', NULL
FROM mool.blds
WHERE path = array['java', 'com', 'rocketfuel', 'grid', 'tools', 'orcinsights', 'RtbidsSizeTracker']

UNION

SELECT id, 'commons-cli', 'commons-cli', '1.3.1', 'test', NULL
FROM mool.blds
WHERE path = array['java', 'com', 'rocketfuel', 'grid', 'onlinestore', 'utils', 'cleanup', 'TestBase']
