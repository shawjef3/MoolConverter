CREATE OR REPLACE VIEW mvn.identifiers AS
WITH base AS (
SELECT
  id AS bld_id,
  coalesce(
    group_id,
    array_to_string(
      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[2:4]
           --for BLDs in java/org/apache/spark
           WHEN path[1:4] = array['java', 'org', 'apache', 'spark'] THEN array['com', 'rocketfuel', 'spark']
           WHEN path[1:3] = array['clojure', 'com', 'rocketfuel'] THEN array['com', 'rocketfuel', 'clojure']
           WHEN path[1] = 'grid' THEN array['com', 'rocketfuel', 'grid2']
           ELSE array_append(array['com', 'rocketfuel'], path[1])
      END,
      '.',
      NULL
    )
  ) AS group_id,
  coalesce(
    artifact_id,
    array_to_string(
      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
           --for BLDs in java/org/apache/spark
           WHEN path[1:4] = array['java', 'org', 'apache', 'spark'] THEN path[5:array_length(path, 1)]
           WHEN path[1:3] = array['clojure', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
           WHEN path[1] = 'grid' THEN array_prepend('grid2', path[2:array_length(path, 1)])
           ELSE path[2:array_length(path, 1)]
      END,
      '.',
      NULL
    )
  ) AS artifact_id,
  coalesce(version, 'M1') AS version,
  CASE WHEN rule_type LIKE '%test' OR classifier = 'tests' THEN 'test-jar' END AS type
  FROM mool_dedup.blds
)
SELECT
  bld_id,
  group_id,
  artifact_id,
  version,
  type
FROM base
GROUP BY bld_id, group_id, artifact_id, version, type
