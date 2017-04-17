package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object Dealias extends Deployable {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE OR REPLACE FUNCTION mool.dealias(bld mool.blds) RETURNS int AS $$
        |DECLARE
        |  dealiased_id int = bld.id;
        |BEGIN
        |  WHILE mool.is_alias(dealiased_id) LOOP
        |    SELECT target_id INTO dealiased_id
        |    FROM mool.bld_to_bld
        |    WHERE source_id = dealiased_id;
        |  END LOOP;
        |
        |  RETURN dealiased_id;
        |END;
        |$$ LANGUAGE plpgsql;
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit = {
    Ignore.ignore("DROP FUNCTION IF EXISTS mool.dealias(mool.blds) CASCADE")
  }
}
