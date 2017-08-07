CREATE TABLE IF NOT EXISTS mvn.exclusions (
  id serial PRIMARY KEY,
  bld_id int NOT NULL,
  dependency_id int NOT NULL,
  excluded_group_id text NOT NULL,
  excluded_artifact_id text NOT NULL
);

CREATE OR REPLACE FUNCTION mvn.create_exclusion(
  bld_id_ int,
  dependency_id_ int,
  excluded_group_id_ text,
  excluded_artifact_id_ text
) RETURNS boolean AS $$

INSERT INTO mvn.exclusions (bld_id, dependency_id, excluded_group_id, excluded_artifact_id)
  VALUES (bld_id_, dependency_id_, excluded_group_id_, excluded_artifact_id_)
RETURNING true;

$$ LANGUAGE sql;

CREATE OR REPLACE FUNCTION mvn.create_exclusion(
  bld_path text[],
  dependency_path text[],
  excluded_group_id text,
  excluded_artifact_id text
) RETURNS boolean AS $$
DECLARE
  bld_id int = mool_dedup.bld_id(bld_path);
  dependency_id int = mool_dedup.bld_id(dependency_path);
BEGIN
  RETURN mvn.create_exclusion(bld_id, dependency_id, excluded_group_id, excluded_artifact_id);
END;
$$ LANGUAGE plpgsql;

/*
If the project depends on org.apache.spark:spark-assembly_2.10,
and com.rocketfuel.modeling:modeling.athena.core.common.Common,
exclude org.mortbay.jetty:servlet-api-2.5 and javax.servlet:servlet-api
from com.rocketfuel.modeling:modeling.athena.core.common.Common.

This is kind of slow.
 */
CREATE OR REPLACE FUNCTION mvn.exclude_servlets(
  conflicting_bld_id int
) RETURNS boolean AS $$
BEGIN
INSERT INTO mvn.exclusions (bld_id, dependency_id, excluded_group_id, excluded_artifact_id)
  WITH requires_spark AS (
    SELECT bld_id
    FROM mool_dedup.dependents_of(ARRAY ['java', 'mvn', 'org', 'apache', 'spark', 'SparkAssembly'])
  ), requires_conflicting AS (
    SELECT bld_id
    FROM mool_dedup.dependents_of(conflicting_bld_id)
  ), spark_and_conflicting AS (
    --bld_ids that depend on spark assembly and athena.core.common.Common.
    SELECT bld_id
    FROM requires_spark
    INTERSECT
    SELECT bld_id
    FROM requires_conflicting
  )
  SELECT
    spark_and_conflicting.bld_id AS bld_id,
    --all dependencies that is or depends on athena.core.Common, from bld_to_blds and dependents_of.
    bld_to_bld.target_id AS dependency_id,
    x.excluded_group_id,
    x.excluded_artifact_id
  FROM spark_and_conflicting
    INNER JOIN mool_dedup.bld_to_bld
      ON spark_and_conflicting.bld_id = bld_to_bld.source_id
         AND bld_to_bld.target_id = conflicting_bld_id
    -- this makes it even slower.
    --      OR EXISTS (
    --     SELECT
    --     FROM mool_dedup.dependencies_of(bld_to_bld.target_id) d
    --     WHERE d.bld_id = conflicting_bld_id
    --      )
    CROSS JOIN (
      VALUES
        ('org.mortbay.jetty', 'servlet-api-2.5'),
        ('javax.servlet', 'servlet-api')
    ) AS x (excluded_group_id, excluded_artifact_id);

  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mvn.exclude_servlets(
  conflicting_bld_path text[]
) RETURNS boolean AS $$
DECLARE
  conflicting_bld_id int = mool_dedup.bld_id(conflicting_bld_path);
BEGIN
  RETURN mvn.exclude_servlets(conflicting_bld_id);
END;
$$ LANGUAGE plpgsql;
