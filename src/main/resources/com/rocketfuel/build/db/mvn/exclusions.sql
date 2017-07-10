/*
If the project depends on org.apache.spark:spark-assembly_2.10,
and com.rocketfuel.modeling:modeling.athena.core.common.Common,
exclude org.mortbay.jetty:servlet-api-2.5 and javax.servlet:servlet-api
from com.rocketfuel.modeling:modeling.athena.core.common.Common.

This is kind of slow.
 */
INSERT INTO mvn.exclusions (bld_id, dependency_id, excluded_group_id, excluded_artifact_id)
--bld_ids that depend on spark assembly and athena.core.common.Common.
WITH require_both AS (
  SELECT bld_id
  FROM mool_dedup.dependents_of(ARRAY ['java', 'mvn', 'org', 'apache', 'spark', 'SparkAssembly'])
  INTERSECT
  SELECT bld_id
  FROM mool_dedup.dependents_of(ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'])
)
SELECT
  bld_id AS bld_id,
  --all dependencies that is or depends on athena.core.Common, from bld_to_blds and dependents_of.
  bld_to_bld.target_id AS dependency_id,
  x.excluded_group_id,
  x.excluded_artifact_id
FROM require_both
INNER JOIN mool_dedup.bld_to_bld
  ON bld_id = bld_to_bld.source_id
  AND bld_to_bld.target_id = mool_dedup.bld_id(ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'])
-- this makes it even slower.
--      OR EXISTS (
--     SELECT
--     FROM mool_dedup.dependencies_of(bld_to_bld.target_id) d
--     WHERE d.bld_id = mool_dedup.bld_id(ARRAY ['java', 'com', 'rocketfuel', 'modeling', 'athena', 'core', 'common', 'Common'])
--      )
CROSS JOIN (
  VALUES
    ('org.mortbay.jetty', 'servlet-api-2.5'),
    ('javax.servlet', 'servlet-api')
  ) AS x (excluded_group_id, excluded_artifact_id)
;
