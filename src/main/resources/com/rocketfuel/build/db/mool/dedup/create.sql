--Some sources are referenced by more than one BLD, causing duplicate classes later on
--when some BLD references the same source through multiple BLDs.

CREATE SCHEMA IF NOT EXISTS mool_dedup;

--these are the sources that need a unique BLD
CREATE OR REPLACE VIEW mool_dedup.source_requiring_dedup AS
  SELECT source_id, array_agg(bld_id) AS bld_ids
  FROM mool.bld_to_sources
  GROUP BY source_id
  HAVING count(*) > 1;

--inverse of mool_dedup.source_requiring_dedup
CREATE OR REPLACE VIEW mool_dedup.blds_for_dedup AS
  SELECT bld_id, source_id
  FROM mool_dedup.source_requiring_dedup, LATERAL unnest(bld_ids) bld_id;

CREATE OR REPLACE VIEW mool_dedup.blds_for_natural_dedup AS
  --these are the already existing unique BLDs for the ambiguous sources
  SELECT source_requiring_dedup.source_id, all_sources.bld_id, source_requiring_dedup.bld_ids
  FROM mool_dedup.source_requiring_dedup
    INNER JOIN mool.bld_to_sources all_sources
      ON all_sources.bld_id IN (SELECT * FROM unnest(source_requiring_dedup.bld_ids))
  GROUP BY source_requiring_dedup.source_id, all_sources.bld_id, source_requiring_dedup.bld_ids
  HAVING count(*) = 1;

CREATE OR REPLACE VIEW mool_dedup.source_requiring_artificial_dedup AS
  WITH remaining AS (
    SELECT source_id
    FROM mool_dedup.source_requiring_dedup
    EXCEPT
    SELECT source_id
    FROM mool_dedup.blds_for_natural_dedup
  )
  SELECT remaining.source_id, bld_ids
  FROM remaining
    INNER JOIN mool_dedup.source_requiring_dedup
      ON remaining.source_id = source_requiring_dedup.source_id;

CREATE OR REPLACE VIEW mool_dedup.blds_for_artificial_dedup AS
  WITH source_paths AS (
      SELECT
        id,
        regexp_split_to_array(path, '/') AS split_path --for getting the file name
      FROM mool.sources
  )
  SELECT
    source_id,
    row_number() OVER (ORDER BY source_id) + (SELECT max(id) FROM mool.blds) AS id,
    --Use the template BLD's path, minus the last node, plus the name of the source file.
    array_append(blds.path[1:array_length(blds.path, 1) - 1], split_path[array_length(split_path, 1)] || '.ambiguous') AS path,
    rule_type,
    scala_version,
    java_version,
    group_id,
    artifact_id,
    version,
    repo_url,
    classifier
  FROM mool_dedup.source_requiring_artificial_dedup art
    INNER JOIN source_paths
      ON art.source_id = source_paths.id
    --Use the first BLD as a template.
    INNER JOIN mool.blds
      ON blds.id = art.bld_ids[1];

CREATE OR REPLACE VIEW mool_dedup.blds AS
  SELECT id, path, rule_type, scala_version, java_version, group_id, artifact_id, version, repo_url, classifier
  FROM mool.blds
  UNION
  SELECT id, path, rule_type, scala_version, java_version, group_id, artifact_id, version, repo_url, classifier
  FROM mool_dedup.blds_for_artificial_dedup;

CREATE OR REPLACE VIEW mool_dedup.bld_to_sources AS
  SELECT id, bld_id, source_id
  FROM mool.bld_to_sources
  WHERE NOT EXISTS (
  --Remove ambiguous sources from all existing BLDs,
      SELECT 1
      FROM mool_dedup.source_requiring_dedup
      WHERE source_requiring_dedup.source_id = bld_to_sources.source_id
  ) OR EXISTS (
        --except natural dedups.
            SELECT 1
            FROM mool_dedup.blds_for_natural_dedup
            WHERE blds_for_natural_dedup.source_id = bld_to_sources.source_id
        ) OR NOT EXISTS (
    --Otherwise keep the mapping.
      SELECT 1
      FROM mool_dedup.source_requiring_dedup
      WHERE bld_to_sources.source_id = source_requiring_dedup.source_id
  )
  UNION
  --Add artificial BLD sources.
  SELECT
    row_number() OVER (ORDER BY source_id) + (SELECT max(id) FROM mool.bld_to_sources) AS id,
    id AS bld_id,
    source_id
  FROM mool_dedup.blds_for_artificial_dedup;

CREATE OR REPLACE VIEW mool_dedup.bld_to_bld AS
  WITH dedup_bld_to_bld AS (
    --Add dependencies to artificial dedup BLDs.
      SELECT
        bld_to_sources.bld_id AS source_id,
        blds_for_artificial_dedup.id AS target_id
      FROM mool_dedup.blds_for_artificial_dedup
        INNER JOIN mool.bld_to_sources
          ON blds_for_artificial_dedup.source_id = bld_to_sources.source_id
    UNION
    --add the natural dependencies to replace ambiguous source dependencies
    SELECT
      source_bld_id,
      bld_id AS target_id
    FROM mool_dedup.blds_for_natural_dedup, LATERAL unnest(array_remove(bld_ids, bld_id)) source_bld_id
  )
  --Keep all the original dependencies.
  SELECT id, source_id, target_id, is_compile
  FROM mool.bld_to_bld
  UNION
  SELECT
    row_number() OVER (ORDER BY dedup_bld_to_bld.source_id, dedup_bld_to_bld.target_id) + (SELECT max(id) FROM mool.bld_to_bld) AS id,
    *,
    false AS is_compile
  FROM dedup_bld_to_bld;

CREATE OR REPLACE VIEW mool_dedup.sources AS
  SELECT * FROM mool.sources;
