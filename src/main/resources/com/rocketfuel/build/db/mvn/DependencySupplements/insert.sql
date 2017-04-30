-- INSERT INTO mvn.dependency_supplements (source_id, group_id, artifact_id, version, scope)
-- SELECT id, 'com.rocketfuel.modeling', 'athena.testdata', 'M1', 'compile'
-- FROM mool.blds
-- WHERE path = array['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsTest']

INSERT INTO mvn.dependency_supplements (source_id, group_id, artifact_id, version, scope)

SELECT id, 'org.scala-lang', 'scala-actors-migration_2.10', '1.0.0', 'compile'
FROM mool.blds
WHERE path = array['java', 'com', 'rocketfuel', 'grid', 'tools', 'orcinsights', 'RtbidsSizeTracker']

UNION

SELECT source.id, target.group_id, target.artifact_id, target.version, 'test'
FROM mool.blds source
CROSS JOIN mool.blds target
WHERE source.path IN (ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'utils', 'UtilsTest'],
                      ARRAY['java', 'com', 'rocketfuel', 'brand', 'veenome', 'aggregation', 'URLSelectionUtilsTests'])
  AND target.path = ARRAY['java', 'mvn', 'org', 'apache', 'spark', 'SparkAssembly']

UNION

SELECT source.id, target.group_id, target.artifact_id, target.version, 'test'
FROM mool.blds source
CROSS JOIN mool.blds target
WHERE source.path = ARRAY['java', 'com', 'rocketfuel', 'brand', 'veenome', 'aggregation', 'URLSelectionUtilsTests']
  AND target.path = ARRAY['java', 'mvn', 'org', 'slf4s', 'Slf4S']
