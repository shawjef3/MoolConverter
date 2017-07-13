CREATE OR REPLACE VIEW mvn.module_paths AS
SELECT
  id,
  array_to_string(
      CASE WHEN path[1] = 'grid' THEN array_prepend('grid2', path[2:array_length(path, 1)])
           WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
           WHEN path[1:3] = array['clojure', 'com', 'rocketfuel'] THEN array_prepend('clojure', path[4:array_length(path, 1)])
           WHEN path[1] = 'java' THEN array_prepend('3rd_party', path[1:array_length(path, 1)])
           ELSE path
      END,
      '/',
      NULL
  ) AS path
FROM mool_dedup.blds
WHERE group_id IS NULL
