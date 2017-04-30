CREATE TABLE mvn.dependency_supplements (
  id serial PRIMARY KEY,
  source_id int NOT NULL,
  group_id text NOT NULL,
  artifact_id text NOT NULL,
  version text NOT NULL,
  scope text NOT NULL
)
