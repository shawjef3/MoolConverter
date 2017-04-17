package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._

/**
  * A mapping from a bld to many blds.
  */
case class BldToBld(
  id: Int,
  sourceId: Int,
  targetId: Int,
  isCompile: Boolean
)

object BldToBld extends Deployable with InsertableToValue[BldToBld] with SelectableById[BldToBld] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE mool.bld_to_bld (
        |  id serial PRIMARY KEY,
        |  source_id int NOT NULL,
        |  target_id int NOT NULL,
        |  is_compile bool NOT NULL,
        |  UNIQUE(source_id, target_id)
        |)
      """.stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mool.bld_to_bld")

  override val insertSql: CompiledStatement =
    """INSERT INTO bld_to_bld (source_id, target_id, is_compile) VALUES (@sourceId, @targetId, @isCompile) RETURNING id"""

  override val selectByIdSql: CompiledStatement =
    """SELECT * FROM bld_to_bld WHERE id = @id"""

}
