CREATE OR REPLACE VIEW mvn.copies AS
WITH dir_parts AS (
    SELECT
      module_paths.path AS module_path,
      sources.path AS source,
      CASE WHEN blds.file_package IS NOT NULL THEN blds.file_package || '/' || (regexp_split_to_array(sources.path, '/'))[array_length(regexp_split_to_array(sources.path, '/'), 1)]
           WHEN sources.path LIKE 'java/%' THEN substring(sources.path from 6 for (char_length(sources.path) - 5))
           WHEN sources.path LIKE 'clojure/%' then substring(sources.path from 8 for (char_length(sources.path) - 7))
           WHEN sources.path LIKE 'grid/%' THEN 'grid2' || substring(sources.path from 5 for (char_length(sources.path)) - 4)
           ELSE sources.path
      END AS package_path,
      CASE WHEN blds.rule_type like '%_test' THEN 'test'
           ELSE 'main'
      END AS config_path,
      CASE WHEN sources.path LIKE '%.clj' THEN 'clojure'
           WHEN sources.path LIKE '%.scala' THEN 'scala'
           WHEN sources.path LIKE '%.java' THEN 'java'
           WHEN sources.path LIKE '%.py' THEN 'python'
           WHEN sources.path LIKE '%.g' THEN 'antlr'
           WHEN sources.path LIKE '%.proto' THEN 'proto'
           WHEN sources.path LIKE '%.thrift' THEN 'thrift'
           WHEN sources.path LIKE '%.c' THEN 'c'
           WHEN sources.path LIKE '%.cc' THEN 'c++'
           ELSE 'resources'
      END AS lang_path
    FROM mool_dedup.blds
      INNER JOIN mvn.module_paths
        ON blds.id = module_paths.id
      INNER JOIN mool_dedup.bld_to_source
        ON blds.id = bld_to_source.bld_id
      INNER JOIN mool.sources
        ON bld_to_source.source_id = sources.id
)
SELECT
  source,
  package_path, --this is for imports in proto files
  array_to_string(array[module_path, 'src', config_path, lang_path, package_path], '/', NULL) AS destination
FROM dir_parts
