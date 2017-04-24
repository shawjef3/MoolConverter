CREATE OR REPLACE VIEW mvn.module_paths AS
SELECT
  id,
  array_to_string(
      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
           WHEN path[1:3] = array['clojure', 'com', 'rocketfuel'] THEN path [4:array_length(path, 1)]
           WHEN path[1:2] = array['java', 'mvn'] THEN array[path[2] || '/' || array_to_string(path[3:array_length(path, 1)], '.', NULL)]
           ELSE path
      END,
      '/',
      NULL
  ) AS path
FROM mool.blds
