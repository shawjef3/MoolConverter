CREATE OR REPLACE VIEW gradle.bld_joins AS (
  SELECT
    b.id,
    b.path AS path,
    b2.id AS added_id,
    'test' AS include_type
  FROM mool_dedup.blds b
    JOIN mool_dedup.bld_to_bld bb ON b.id = bb.target_id
    JOIN mool_dedup.blds b2 ON bb.source_id = b2.id
  WHERE b.rule_type IN ('java_lib', 'java_bin', 'scala_lib', 'scala_bin')
        AND b2.rule_type IN ('java_test', 'scala_test')
        AND array_to_string(b.path, '-') || 'Test' = array_to_string(b2.path, '-')
)