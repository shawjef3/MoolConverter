package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._

case class RelCfgToBld(
  id: Int,
  relcfgId: Int,
  bldId: Int,
  withDeps: Boolean,
  path: String
) {

}

object RelCfgToBld extends Deployable with InsertableToValue[RelCfgToBld] with SelectableById[BldToBld] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE mool.relcfg_to_bld (
        |  id serial PRIMARY KEY,
        |  relcfg_id int NOT NULL,
        |  bld_id int NOT NULL,
        |  with_deps bool NOT NULL,
        |  path text NOT NULL,
        |  UNIQUE(relcfg_id, bld_id)
        |)
      """.stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("""DROP TABLE IF EXISTS mool.relcfg_to_bld""")

  override val insertSql: CompiledStatement =
    """INSERT INTO mool.relcfg_to_bld (
      |  relcfg_id,
      |  bld_id,
      |  with_deps,
      |  path
      |) VALUES (
      |  @relcfgId,
      |  @bldId,
      |  @withDeps,
      |  @path
      |) RETURNING id
      |""".stripMargin

  override val selectByIdSql: CompiledStatement =
    """SELECT *
      |FROM mool.relcfg_to_bld
      |WHERE id = @id
      |""".stripMargin

}
