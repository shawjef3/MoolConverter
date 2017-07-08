CREATE OR REPLACE VIEW gradle.build_tree AS (
  WITH RECURSIVE build_tree(id, path, bld_id, bld_path,
    rule_type,
    scala_version,
    java_version,
    group_id,
    artifact_id,
    version,
    repo_url,
    classifier
  ) AS (
    SELECT r.id AS id, r.path AS path, b.id AS bld_id, b.path AS bld_path,
      b.rule_type,
      b.scala_version,
      b.java_version,
      b.group_id,
      b.artifact_id,
      b.version,
      b.repo_url,
      b.classifier
    FROM mool.blds b
      JOIN mool.relcfg_to_bld rb ON b.id = rb.bld_id
      JOIN mool.relcfgs r ON rb.relcfg_id = r.id
  UNION
    SELECT bt.id AS id, bt.path AS path, b.id AS bld_id, b.path AS bld_path,
      b.rule_type,
      b.scala_version,
      b.java_version,
      b.group_id,
      b.artifact_id,
      b.version,
      b.repo_url,
      b.classifier
    FROM build_tree bt
      JOIN mool.bld_to_bld bb ON bt.bld_id = bb.source_id
      JOIN mool.blds b ON bb.target_id = b.id
  )
SELECT *
FROM build_tree
ORDER BY path, bld_path)
