package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._

/**
  * A mapping from a bld to many blds.
  */
case class BldToDep(
  id: Int,
  source: Int,
  target: Int,
  isCompile: Boolean
)

object BldToDep extends Deployable with InsertableToValue[BldToDep] with SelectableById[BldToDep] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore(
      """CREATE TABLE bld_to_bld (
        |  id serial PRIMARY KEY,
        |  source_id int NOT NULL,
        |  target_id int NOT NULL,
        |  is_compile bool NOT NULL,
        |  UNIQUE(source_id, target_id)
        |)
      """.stripMargin
    ).ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore("DROP TABLE IF EXISTS bld_to_bld").ignore()

  override val insertSql: CompiledStatement =
    """INSERT INTO bld_to_bld (source, target, is_compile) VALUES (@source, @target, @isCompile)"""

  override val selectByIdSql: CompiledStatement =
    """SELECT * FROM bld_to_bld WHERE id = @id"""

}
