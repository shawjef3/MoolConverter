CREATE OR REPLACE VIEW mvn.dependencies AS
WITH has_duplicates AS (
  SELECT
    bld_to_bld.source_id,
    bld_to_bld.target_id,
    identifiers.group_id,
    identifiers.artifact_id,
    identifiers.version,
    CASE WHEN blds.rule_type like '%_test' THEN 'test' --BLDs only have one scope
         WHEN bld_to_bld.is_compile THEN 'provided'
         ELSE 'compile'
    END AS scope
  FROM mool.bld_to_bld
  INNER JOIN mool.blds
    ON bld_to_bld.target_id = blds.id
  INNER JOIN mvn.identifiers
    ON identifiers.bld_id = bld_to_bld.target_id
), agg_scopes AS (
  SELECT
    source_id,
    max(target_id) AS target_id,
    group_id,
    artifact_id,
    version,
    array_position(array_agg(scope), 'provided') IS NOT NULL AS is_provided,
    array_position(array_agg(scope), 'test') IS NOT NULL AS is_test,
    array_position(array_agg(scope), 'compile') IS NOT NULL AS is_compile
  FROM has_duplicates
  GROUP BY source_id, group_id, artifact_id, version
)
SELECT
  source_id,
  target_id,
  group_id,
  artifact_id,
  version,
  CASE WHEN is_compile THEN 'compile'
       WHEN is_provided THEN 'provided'
       WHEN is_test THEN 'test'
  END AS scope
FROM agg_scopes
