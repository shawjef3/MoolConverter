CREATE OR REPLACE VIEW mvn.all_dependencies AS
WITH a AS (
  SELECT *
  FROM mvn.dependencies
  UNION
  SELECT
    source_id :: int,
    NULL :: int AS target_Id,
    group_id :: text,
    artifact_id :: text,
    version :: text,
    scope :: text,
    type :: text
  FROM mvn.provided_dependencies
)
SELECT
  source_id,
  min(target_id) AS target_id,
  group_id,
  artifact_id,
  max(version) AS version,
  min(scope) AS scope, --compile < test
  type
FROM a
GROUP BY source_id, group_id, artifact_id, type
