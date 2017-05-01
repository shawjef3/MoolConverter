/*
These are dependencies that need to be added to test projects.
Maven does not automatically add "provided" dependencies to
tests, as mool does for compileDeps.
*/
CREATE OR REPLACE VIEW mvn.provided_dependencies AS
WITH RECURSIVE recursive_dependencies (
    source_id,
    target_id, --the upstream dependency
    group_id,
    artifact_id,
    version,
    type
) AS (
  --The immediate dependency's compile dependencies.
  SELECT
    source.id,
    source_dependencies.target_id,
    target_dependencies.group_id,
    target_dependencies.artifact_id,
    target_dependencies.version,
    target_dependencies.type
  FROM mool.blds source --source is the test bld
  INNER JOIN mvn.dependencies source_dependencies
    ON source.id = source_dependencies.source_id
  LEFT OUTER JOIN mvn.dependencies target_dependencies --targets are the upstream blds
    ON source_dependencies.target_id = target_dependencies.source_id
       AND target_dependencies.scope = 'provided'
  WHERE source.rule_type LIKE '%test'

  UNION

  --the upstream dependency's compile dependencies
  SELECT
    source.source_id,
    target_dependencies.target_id,
    target_target_dependencies.group_id,
    target_target_dependencies.artifact_id,
    target_target_dependencies.version,
    target_target_dependencies.type
  FROM recursive_dependencies source
  INNER JOIN mvn.dependencies target_dependencies
    ON source.target_id = target_dependencies.source_id
  LEFT OUTER JOIN mvn.dependencies target_target_dependencies --targets are the upstream blds
    ON target_dependencies.target_id = target_target_dependencies.source_id
       AND target_dependencies.scope = 'provided'
)
SELECT
  source_id,
  group_id,
  artifact_id,
  version,
  'compile' AS scope, --could be 'test', but that wouldn't be transitive
  type
FROM recursive_dependencies
WHERE group_id IS NOT NULL
