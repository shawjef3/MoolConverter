CREATE OR REPLACE VIEW mvn.dependencies AS
WITH has_duplicates AS (
  SELECT
    bld_to_bld.source_id,
    bld_to_bld.target_id,
    target.path AS target_path,
    identifiers.group_id,
    identifiers.artifact_id,
    identifiers.version,
    CASE WHEN source.rule_type LIKE '%test' THEN 'compile' --could be 'test', but that wouldn't be transitive
         WHEN bld_to_bld.is_compile THEN 'provided'
         ELSE 'compile'
    END AS scope,
    identifiers.type
  FROM mool.bld_to_bld
  INNER JOIN mool.blds source
    ON bld_to_bld.source_id = source.id
  INNER JOIN mool.blds target
    ON bld_to_bld.target_id = target.id
  INNER JOIN mvn.identifiers
    ON identifiers.bld_id = bld_to_bld.target_id
), agg_scopes AS (
  SELECT
    source_id,
    max(target_id) AS target_id,
    max(target_path) AS target_path,
    group_id,
    artifact_id,
    version,
    array_position(array_agg(scope), 'provided') IS NOT NULL AS is_provided,
    array_position(array_agg(scope), 'test') IS NOT NULL AS is_test,
    array_position(array_agg(scope), 'compile') IS NOT NULL AS is_compile,
    type
  FROM has_duplicates
  GROUP BY source_id, group_id, artifact_id, version, type
), single_scope AS (
SELECT
  source_id,
  target_id,
  target_path,
  group_id,
  artifact_id,
  version,
  CASE WHEN is_test THEN 'compile' --could be 'test', but that wouldn't be transitive
       WHEN is_compile THEN 'compile'
       WHEN is_provided THEN 'provided'
  END AS scope,
  type
FROM agg_scopes
)
SELECT
  source_id,
  target_id,
  group_id,
  artifact_id,
  version,
  scope,
  type
FROM single_scope
WHERE NOT (group_id = 'com.rocketfuel.java' AND target_path = array['java', 'mvn', 'com', 'google', 'protobuf', 'ProtobufJava250'])
UNION
SELECT
  source_id,
  target_id,
  'com.google.protobuf',
  'protobuf-java',
  '2.5.0',
  scope,
  type
FROM single_scope
WHERE group_id = 'com.rocketfuel.java' AND target_path = array['java', 'mvn', 'com', 'google', 'protobuf', 'ProtobufJava250']
UNION
SELECT source_id, NULL, group_id, artifact_id, version, scope, type
FROM mvn.dependency_supplements
