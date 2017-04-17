CREATE OR REPLACE VIEW mvn.copies AS
WITH dir_parts AS (
    SELECT
      module_paths.path AS module_path,
      sources.path AS source,
      CASE WHEN blds.rule_type like '%_test' THEN 'test'
           ELSE 'main'
      END AS config_path,
      CASE WHEN sources.path LIKE '%.scala' THEN 'scala'
           WHEN sources.path LIKE '%.java' THEN 'java'
           WHEN sources.path LIKE '%.py' THEN 'python'
           ELSE 'resources'
      END AS lang_path
    FROM mool.blds
      INNER JOIN mvn.module_paths
        ON blds.id = module_paths.id
      INNER JOIN mool.bld_to_sources
        ON blds.id = bld_to_sources.bld_id
      INNER JOIN mool.sources
        ON bld_to_sources.source_id = sources.id
)
SELECT
  source,
  module_path || array_to_string(array['', 'src', config_path, lang_path, ''], '/', NULL) ||
  CASE WHEN source LIKE 'java/%' THEN substring(source from 6 for (char_length(source) - 5))
       ELSE source
  END AS destination
FROM dir_parts