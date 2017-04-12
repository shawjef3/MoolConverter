package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._

case class BldToSource(
  id: Int,
  bldId: Int,
  sourceId: Int
)

object BldToSource extends Deployable with InsertableToValue[BldToSource] with SelectableById[BldToSource] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore(
      """CREATE TABLE bld_to_sources (
      |  id serial PRIMARY KEY,
      |  bld_id int NOT NULL,
      |  source_id int NOT NULL,
      |  UNIQUE(bld_id, source_id)
      |)
      |""".stripMargin
    ).ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore("DROP TABLE IF EXISTS bld_to_sources").ignore()

  override val insertSql: CompiledStatement =
    """INSERT INTO bld_to_sources (
      |  bld_id,
      |  source_id
      |) VALUES (
      |  @bldId,
      |  @sourceId
      |) RETURNING id
      |""".stripMargin

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM bld_to_sources WHERE id = @id"

  val selectByBldId: Select[BldToSource] =
    Select("SELECT * FROM bld_to_sources WHERE bld_id = @bld_id")

  def selectByBldId(bldId: Int)(implicit connection: Connection): Vector[BldToSource] =
    selectByBldId.on("bld_id" -> bldId).vector()

  val selectBySourceId: Select[BldToSource] =
    Select("SELECT * FROM bld_to_sources WHERE source_id = @source_id")

  def selectBySourceId(sourceId: Int)(implicit connection: Connection): Vector[BldToSource] =
    selectByBldId.on("source_id" -> sourceId).vector()
}
