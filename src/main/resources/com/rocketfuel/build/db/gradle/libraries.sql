CREATE OR REPLACE VIEW gradle.libraries AS (
  SELECT DISTINCT
    b.id,
    array_to_string(b.path, '.') AS path,
    b.rule_type,
    b.scala_version,
    b.java_version,
    b.group_id,
    b.artifact_id,
    b.version,
    b.repo_url,
    b.classifier
  FROM gradle.build_tree bt
    JOIN mool_dedup.blds b ON bt.bld_id = b.id
)
