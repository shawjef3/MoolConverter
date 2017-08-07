CREATE OR REPLACE VIEW gradle.project_mapping AS (
	SELECT bld_id,
		array_to_string(bld_path, '-') AS bld_path,
		string_agg(DISTINCT array_to_string(path, '-'), ',') AS prj_path,
		rule_type,
		scala_version,
		java_version,
		group_id,
		artifact_id,
		version,
		repo_url,
		classifier

	FROM gradle.build_tree
	GROUP BY bld_id, array_to_string(bld_path, '-'),
		rule_type,
		scala_version,
		java_version,
		group_id,
		artifact_id,
		version,
		repo_url,
		classifier
)
