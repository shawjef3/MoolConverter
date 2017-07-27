CREATE OR REPLACE VIEW gradle.ignored_blds AS (
  SELECT b.id, array_to_string(b.path, '-') AS path
  FROM mool_dedup.blds b
  WHERE b.rule_type IN ('py_lib','py_bin', 'py_test', 'cc_bin', 'cc_bin', 'cc_test', 'release_package')
        AND NOT EXISTS (SELECT * FROM mool_dedup.bld_to_bld bb WHERE b.id = bb.target_id)
)
