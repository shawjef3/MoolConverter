package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._

case class BldToSource(
  id: Int,
  bldId: Int,
  sourceId: Int
)

object BldToSource extends Deployable with InsertableToValue[BldToSource] with SelectableById[BldToSource] {
  val list =
    Select[BldToSource]("SELECT id, bld_id bldId, source_id sourceId FROM mool.bld_to_sources")

  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE mool.bld_to_sources (
      |  id serial PRIMARY KEY,
      |  bld_id int NOT NULL,
      |  source_id int NOT NULL,
      |  UNIQUE(bld_id, source_id)
      |)
      |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mool.bld_to_sources")

  override val insertSql: CompiledStatement =
    """INSERT INTO mool.bld_to_sources (
      |  bld_id,
      |  source_id
      |) VALUES (
      |  @bldId,
      |  @sourceId
      |) RETURNING id
      |""".stripMargin

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM mool.bld_to_sources WHERE id = @id"

  val selectByBldId: Select[BldToSource] =
    Select("SELECT * FROM mool.bld_to_sources WHERE bld_id = @bld_id")

  def selectByBldId(bldId: Int)(implicit connection: Connection): Vector[BldToSource] =
    selectByBldId.on("bld_id" -> bldId).vector()

  val selectBySourceId: Select[BldToSource] =
    Select("SELECT * FROM mool.bld_to_sources WHERE source_id = @source_id")

  def selectBySourceId(sourceId: Int)(implicit connection: Connection): Vector[BldToSource] =
    selectByBldId.on("source_id" -> sourceId).vector()

}
