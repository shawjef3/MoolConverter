CREATE OR REPLACE VIEW gradle.dependencies AS (
  SELECT DISTINCT
    bt.id                         AS prj_id,
    array_to_string(bt.path, '-') AS prj_path,
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
  FROM gradle.build_tree bt
    JOIN mool.bld_to_bld bb ON bt.bld_id = bb.source_id
    JOIN mool.blds b ON bb.target_id = b.id
)