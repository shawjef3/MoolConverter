/*
Removes one layer of aliasing.
An alias is a BLD which merely references another bld. Mool sometimes does this and filters files in the source artifact.
 */
CREATE OR REPLACE VIEW mool.bld_aliases AS
  WITH source_counts AS (
      SELECT blds.id bld_id, count(bld_to_sources.bld_id)
      FROM mool.blds
        LEFT OUTER JOIN mool.bld_to_sources
          ON blds.id = bld_to_sources.bld_id
      GROUP BY blds.id
  ),
  local_dep_counts AS (
    SELECT blds.id bld_id, count(*)
    FROM mool.blds
      INNER JOIN mool.bld_to_bld
        ON bld_to_bld.source_id = blds.id
    GROUP BY blds.id
  )
  SELECT blds.id AS source_id, bld_to_bld.target_id
  FROM mool.blds
    INNER JOIN mool.bld_to_bld
      ON blds.id = bld_to_bld.source_id
  WHERE blds.artifact_id IS NULL AND
    blds.group_id IS NULL AND
    blds.version IS NULL
    AND EXISTS (
        SELECT 1
        FROM source_counts
        WHERE blds.id = source_counts.bld_id
              AND source_counts.count = 0
    ) AND EXISTS (
        SELECT 1
        FROM local_dep_counts
        WHERE blds.id = local_dep_counts.bld_id
              AND local_dep_counts.count = 1
    )
