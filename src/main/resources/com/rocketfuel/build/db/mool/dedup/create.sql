CREATE SCHEMA mool_dedup;

CREATE VIEW mool_dedup.requiring_dedup AS
  WITH ambiguous AS (
    SELECT source_id, array_agg(bld_id) bld_ids
    FROM mool.bld_to_sources
    GROUP BY source_id
    HAVING count(*) > 1
  ), flattened AS (
    select *
    from ambiguous, lateral unnest(bld_ids) bld_id
  )
  select source_id, bld_id, blds.path as bld_path, sources.path as source_path
  from flattened
    inner join mool.sources
      on source_id = sources.id
    inner join mool.blds
      on bld_id = blds.id
  --where rule_type like 'java%' or rule_type like 'scala%'
  order by source_id, bld_id;

CREATE TABLE mool_dedup.bld_additions (
  id int NOT NULL DEFAULT nextval('mool.blds_id_seq') PRIMARY KEY,
  path text[] NOT NULL UNIQUE,
  rule_type text NOT NULL,
  scala_version text,
  java_version text,
  group_id text,
  artifact_id text,
  version text,
  repo_url text,
  classifier text
);

CREATE TABLE mool_dedup.bld_removals (
  bld_id int NOT NULL
);

CREATE INDEX ON mool_dedup.bld_removals (bld_id);

CREATE OR REPLACE VIEW mool_dedup.blds AS
  WITH ids AS (
    SELECT id
    FROM mool.blds
    EXCEPT
    SELECT bld_id
    FROM mool_dedup.bld_removals
  )
  SELECT *
  FROM mool.blds
  WHERE id in (SELECT id FROM ids)
  UNION
  SELECT *
  FROM mool_dedup.bld_additions;

CREATE TABLE mool_dedup.bld_to_source_removals (
  bld_to_source_id int NOT NULL
);

CREATE TABLE mool_dedup.bld_to_source_additions (
  id int DEFAULT nextval('mool.bld_to_sources_id_seq') PRIMARY KEY,
  bld_id int NOT NULL,
  source_id int NOT NULL
);

CREATE OR REPLACE VIEW mool_dedup.bld_to_source AS
  WITH ids AS (
    SELECT id
    FROM mool.bld_to_sources
    EXCEPT
    SELECT bld_to_source_id
    FROM mool_dedup.bld_to_source_removals
  )
  SELECT *
  FROM mool.bld_to_sources
  WHERE id in (SELECT id FROM ids)
  UNION
  SELECT *
  FROM mool_dedup.bld_to_source_additions;

CREATE TABLE mool_dedup.bld_to_bld_removals (
  bld_to_bld_id int NOT NULL
);

CREATE TABLE mool_dedup.bld_to_bld_additions (
  id int DEFAULT nextval('mool.bld_to_bld_id_seq') PRIMARY KEY,
  source_id int NOT NULL,
  target_id int NOT NULL,
  is_compile bool NOT NULL
);

CREATE OR REPLACE VIEW mool_dedup.bld_to_bld AS
  WITH ids AS (
    SELECT id
    FROM mool.bld_to_bld
    EXCEPT
    SELECT bld_to_bld_id
    FROM mool_dedup.bld_to_bld_removals
  )
  SELECT *
  FROM mool.bld_to_bld
  WHERE id in (SELECT id FROM ids)
  UNION
  SELECT *
  FROM mool_dedup.bld_to_bld_additions;

CREATE OR REPLACE FUNCTION mool_dedup.remove_dependency(remove_source_id int, remove_target_id int) RETURNS bool AS $$
BEGIN
  INSERT INTO mool_dedup.bld_to_bld_removals (bld_to_bld_id)
    SELECT id
    FROM mool.bld_to_bld
    WHERE source_id = remove_source_id
          AND target_id = remove_target_id;

  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.remove_dependency(remove_source_path text[], remove_target_path text[]) RETURNS bool AS $$
DECLARE
  remove_source_id int;
  remove_target_id int;
BEGIN
  SELECT id INTO STRICT remove_source_id
  FROM mool.blds
  WHERE path = remove_source_path;

  SELECT id INTO STRICT remove_target_id
  FROM mool.blds
  WHERE path = remove_target_path;

  RETURN mool_dedup.remove_dependency(remove_source_id, remove_target_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.add_dependency(add_source_id int, add_target_id int, is_compile boolean) RETURNS bool AS $$
BEGIN
  INSERT INTO mool_dedup.bld_to_bld_additions (source_id, target_id, is_compile)
  VALUES (add_source_id, add_target_id, is_compile);
  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.add_dependency(add_source_path text[], add_target_path text[], is_compile boolean) RETURNS bool AS $$
DECLARE
  add_source_id int;
  add_target_id int;
BEGIN
  SELECT id INTO STRICT add_source_id
  FROM mool.blds
  WHERE path = add_source_path;

  SELECT id INTO STRICT add_target_id
  FROM mool.blds
  WHERE path = add_target_path;

  RETURN mool_dedup.add_dependency(add_source_id, add_target_id, is_compile);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.move_dependency(old_source_id int, new_source_id int, move_target_id int) RETURNS bool AS $$
BEGIN
  INSERT INTO mool_dedup.bld_to_bld_additions (source_id, target_id, is_compile)
    SELECT new_source_id, move_target_id, is_compile
    FROM mool.bld_to_bld
    WHERE source_id = old_source_id
          AND target_id = move_target_id;

  PERFORM mool_dedup.remove_dependency(old_source_id, move_target_id);

  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.move_dependency(old_source_path text[], new_source_path text[], move_target_path text[]) RETURNS bool AS $$
DECLARE
  old_source_id int;
  new_source_id int;
  move_target_id int;
BEGIN
  SELECT id INTO STRICT old_source_id
  FROM mool.blds
  WHERE path = old_source_path;

  SELECT id INTO STRICT new_source_id
  FROM mool.blds
  WHERE path = new_source_path;

  SELECT id INTO STRICT move_target_id
  FROM mool.blds
  WHERE path = move_target_path;

  RETURN mool_dedup.move_dependency(old_source_id, new_source_id, move_target_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.remove_source(remove_bld_id int, remove_source_id int) RETURNS bool AS $$
BEGIN
  INSERT INTO mool_dedup.bld_to_source_removals (bld_to_source_id)
    SELECT id
    FROM mool.bld_to_sources
    WHERE source_id = remove_source_id
          AND bld_id = remove_bld_id;
  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.remove_source(remove_bld_path text[], remove_source_path text) RETURNS bool AS $$
DECLARE
  remove_bld_id int;
  remove_source_id int;
BEGIN
  SELECT id INTO STRICT remove_bld_id
  FROM mool.blds
  WHERE path = remove_bld_path;

  SELECT id INTO STRICT remove_source_id
  FROM mool.sources
  WHERE path = remove_source_path;

  RETURN mool_dedup.remove_source(remove_bld_id, remove_source_id);
END;
$$ language plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.add_source(
  bld_id int,
  source_id int
) RETURNS int AS $$
DECLARE
  bld_to_source_id int;
BEGIN

INSERT INTO mool_dedup.bld_to_source_additions (bld_id, source_id)
  VALUES (bld_id, source_id)
  RETURNING id INTO STRICT bld_to_source_id;

  RETURN bld_to_source_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.add_source(
  bld_path text[],
  source_path text
) RETURNS int AS $$
DECLARE
  bld_id int;
  source_id int;
BEGIN

  SELECT id INTO STRICT bld_id
  FROM mool_dedup.blds
  WHERE path = bld_path;

  SELECT id INTO STRICT source_id
  FROM mool.sources
  WHERE path = source_path;

  RETURN mool_dedup.add_source(bld_id, source_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.move_source(remove_bld_id int, add_bld_id int, move_source_id int) RETURNS bool AS $$
BEGIN
  INSERT INTO mool_dedup.bld_to_source_additions (bld_id, source_id)
    VALUES (add_bld_id, move_source_id);

  PERFORM mool_dedup.remove_source(remove_bld_id, move_source_id);

  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.move_source(remove_bld_path text[], add_bld_path text[], move_source_path text) RETURNS bool AS $$
DECLARE
  remove_bld_id int;
  add_bld_id int;
  move_source_id int;
BEGIN
  SELECT id INTO STRICT remove_bld_id
  FROM mool.blds
  WHERE path = remove_bld_path;

  SELECT id INTO STRICT add_bld_id
  FROM mool.blds
  WHERE path = add_bld_path;

  SELECT id INTO STRICT move_source_id
  FROM mool.sources
  WHERE path = move_source_path;

  RETURN mool_dedup.move_source(remove_bld_id, add_bld_id, move_source_id);
END;
$$ language plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.remove_bld(remove_bld_id int) RETURNS boolean AS $$
BEGIN

  INSERT INTO mool_dedup.bld_to_source_removals (bld_to_source_id)
    SELECT id
    FROM mool.bld_to_sources
    WHERE bld_id = remove_bld_id;

  INSERT INTO mool_dedup.bld_to_bld_removals (bld_to_bld_id)
    SELECT id
    FROM mool.bld_to_bld
    WHERE source_id = remove_bld_id;

  INSERT INTO mool_dedup.bld_to_bld_removals (bld_to_bld_id)
    SELECT id
    FROM mool.bld_to_bld
    WHERE target_id = remove_bld_id;

  INSERT INTO mool_dedup.bld_removals (bld_id)
  VALUES (remove_bld_id);

  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.remove_bld(remove_bld_path text[]) RETURNS boolean AS $$
DECLARE
  remove_bld_id int;
BEGIN
  SELECT id INTO STRICT remove_bld_id
  FROM mool.blds
  WHERE path = remove_bld_path;

  RETURN mool_dedup.remove_bld(remove_bld_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.copy_bld(original_bld_id int, new_path text[]) RETURNS int AS $$
DECLARE
  new_bld_id int;
BEGIN

  INSERT INTO mool_dedup.bld_additions (
    path,
    rule_type,
    scala_version,
    java_version,
    group_id,
    artifact_id,
    version,
    repo_url,
    classifier
  )
    SELECT
      new_path,
      rule_type,
      scala_version,
      java_version,
      group_id,
      artifact_id,
      version,
      repo_url,
      classifier
    FROM mool.blds
    WHERE id = original_bld_id
  RETURNING bld_additions.id INTO new_bld_id;

  RETURN new_bld_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.copy_bld(original_bld_path text[], new_path text[]) RETURNS int AS $$
DECLARE
  original_bld_id int;
BEGIN

  SELECT id INTO STRICT original_bld_id
  FROM mool.blds
  WHERE path = original_bld_path;

  RETURN mool_dedup.copy_bld(original_bld_id, new_path);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.factor(parent0_id int, parent1_id int, new_parent text[]) RETURNS boolean AS $$
DECLARE
  new_parent_id int;
BEGIN
  --create a bld for the common dependencies and sources, using parent0 as a template
  new_parent_id = mool_dedup.copy_bld(parent0_id, new_parent);

  --add the new bld to source mappings (common sources)
  PERFORM mool_dedup.move_source(
    parent0_id,
    new_parent_id,
    source_id
  )
  FROM (
         SELECT source_id
         FROM mool.bld_to_sources
         WHERE bld_id = parent0_id
         INTERSECT
         SELECT source_id
         FROM mool.bld_to_sources
         WHERE bld_id = parent1_id
       ) sources;

--   add the new bld to bld mappings (common dependencies)
  PERFORM mool_dedup.move_dependency(
    parent0_id,
    new_parent_id,
    target_id
  )
    FROM (
           SELECT target_id
           FROM mool.bld_to_bld
           WHERE source_id = parent0_id
           INTERSECT
           SELECT target_id
           FROM mool.bld_to_bld
           WHERE source_id = parent1_id
         ) targets;

  --The original BLDs depend on the new one.
  INSERT INTO mool_dedup.bld_to_bld_additions (source_id, target_id, is_compile)
    VALUES (parent0_id, new_parent_id, false), (parent1_id, new_parent_id, false);

  RETURN true;

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.factor(parent0 text[], parent1 text[], new_parent text[]) RETURNS boolean AS $$
DECLARE
  parent0_id int;
  parent1_id int;
BEGIN
  SELECT id INTO STRICT parent0_id FROM mool.blds WHERE path = parent0;
  SELECT id INTO STRICT parent1_id FROM mool.blds WHERE path = parent1;

  RETURN mool_dedup.factor(parent0_id, parent1_id, new_parent);
END;
$$ LANGUAGE plpgsql;

--Move all dependencies and sources from one bld to another, and remove the original.
CREATE OR REPLACE FUNCTION mool_dedup.factor_into(parent0_id int, parent1_id int) RETURNS boolean AS $$
BEGIN
  PERFORM mool_dedup.move_source(
    parent0_id,
    parent1_id,
    bld_to_sources.source_id
  )
  FROM mool.bld_to_sources
  WHERE bld_id = parent0_id;

  PERFORM mool_dedup.move_dependency(
    parent0_id,
    parent1_id,
    bld_to_bld.target_id
  )
  FROM mool.bld_to_bld
  WHERE source_id = parent0_id;

  PERFORM mool_dedup.remove_bld(parent0_id);

  RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mool_dedup.factor_into(parent0_path text[], parent1_path text[]) RETURNS boolean AS $$
DECLARE
  parent0_id int;
  parent1_id int;
BEGIN

  SELECT id INTO STRICT parent0_id
  FROM mool_dedup.blds WHERE path = parent0_path;

  SELECT id INTO STRICT parent1_id
  FROM mool_dedup.blds WHERE path = parent1_path;

  RETURN mool_dedup.factor_into(parent0_id, parent1_id);

END;
$$ LANGUAGE plpgsql;
