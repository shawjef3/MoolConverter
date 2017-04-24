package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object IsAlias extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE OR REPLACE FUNCTION mool.is_alias(bld mool.blds) RETURNS bool AS $$
        |DECLARE
        |  src_count int;
        |  dep_count int;
        |BEGIN
        |  SELECT count(*) INTO src_count
        |  FROM mool.bld_to_sources
        |  GROUP BY bld_id
        |  HAVING bld_id = bld.id;
        |
        |  SELECT count(*) INTO dep_count
        |  FROM mool.bld_to_bld
        |  GROUP BY source_id
        |  HAVING source_id = bld.id;
        |
        |  RETURN coalesce(0, src_count) = 0 AND coalesce(dep_count, 0) = 1 AND bld.artifact_id IS NULL AND bld.group_id IS NULL AND bld.version IS NULL;
        |END;
        |$$ LANGUAGE plpgsql;
        |
        |CREATE OR REPLACE FUNCTION mool.is_alias(bld_id int) RETURNS bool AS $$
        |DECLARE
        |  bld mool.blds%rowtype;
        |BEGIN
        |  SELECT * INTO bld
        |  FROM mool.blds
        |  WHERE bld_id = id;
        |
        |  RETURN mool.is_alias(bld);
        |END;
        |$$ LANGUAGE plpgsql;
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit = {
    Ignore.ignore("DROP FUNCTION IF EXISTS mool.is_alias(int) CASCADE")
    Ignore.ignore("DROP FUNCTION IF EXISTS mool.is_alias(mool.blds) CASCADE")
  }
}
