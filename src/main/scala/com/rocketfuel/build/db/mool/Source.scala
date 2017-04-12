package com.rocketfuel.build.db.mool

import com.rocketfuel.build.db._
import com.rocketfuel.sdbc.PostgreSql._
import java.sql.SQLException

case class Source(
  id: Int,
  path: String
)

object Source extends Deployable with InsertableToValue[Source] with SelectableById[Source] {
  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE sources (
        |  id serial PRIMARY KEY,
        |  path text NOT NULL UNIQUE
        |)
        |""".stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS sources")

  override val insertSql: CompiledStatement =
    """INSERT INTO sources (
      |  path
      |) VALUES (
      |  @path
      |)
      |RETURNING id
    """.stripMargin

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM sources WHERE id = @id"

  val selectByPath: CompiledStatement =
    "SELECT * FROM sources WHERE path = @path"

  def selectByPath(path: String)(implicit connection: Connection): Option[Source] =
    Select[Source](selectByPath).on("path" -> path).option()

  def insertOrSelect(path: String)(implicit connection: Connection): Source = {
    try {
      insert(Source(0, path))
    } catch {
      case _: SQLException =>
        selectByPath(path).get
    }
  }
}
