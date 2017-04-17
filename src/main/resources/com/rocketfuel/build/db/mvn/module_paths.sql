CREATE VIEW mvn.module_paths AS
SELECT
  id,
  array_to_string(
      CASE WHEN path[1:3] = array['java', 'com', 'rocketfuel'] THEN path[4:array_length(path, 1)]
           ELSE path
      END,
      '/',
      NULL
  ) AS path
FROM mool.blds
WHERE array[path[1], path[2]] != array['java', 'mvn']
