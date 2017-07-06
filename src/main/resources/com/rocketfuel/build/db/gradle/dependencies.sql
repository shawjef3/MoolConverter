CREATE OR REPLACE VIEW gradle.dependencies AS (
  SELECT DISTINCT
    pm.prj_path,
    b.id,
    array_to_string(b.path, '.')  AS path,
    b.rule_type,
    b.scala_version,
    b.java_version,
    b.group_id,
    b.artifact_id,
    b.version,
    b.repo_url,
    b.classifier,
    bb.is_compile
  FROM gradle.project_mapping pm
    JOIN mool_dedup.bld_to_bld bb ON pm.bld_id = bb.source_id
    JOIN mool_dedup.blds b ON bb.target_id = b.id
  UNION DISTINCT
  SELECT DISTINCT
    pm.prj_path,
    b.id,
    array_to_string(b.path, '.')  AS path,
    b.rule_type,
    b.scala_version,
    b.java_version,
    b.group_id,
    b.artifact_id,
    b.version,
    b.repo_url,
    b.classifier,
    false
  FROM gradle.project_mapping pm
    JOIN mool_dedup.blds b ON pm.bld_id = b.id
)