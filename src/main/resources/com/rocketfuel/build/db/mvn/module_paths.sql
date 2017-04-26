CREATE OR REPLACE VIEW mvn.module_paths AS
SELECT
  id,
  array_to_string(
      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
           WHEN path[1:3] = array['clojure', 'com', 'rocketfuel'] THEN array['clojure', array_to_string(path [4:array_length(path, 1)], '.', NULL)]
           WHEN path[1] = 'java' THEN array_prepend('3rd party', path[1:array_length(path, 1)])
           ELSE path
      END,
      '/',
      NULL
  ) AS path
FROM mool.blds
WHERE group_id IS NULL
