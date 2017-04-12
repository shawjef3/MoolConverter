package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

case class Source(
  path: String
)

object Source extends Deployable with InsertableToValue[Source] {

  override val insertSql: CompiledStatement =
    """INSERT INTO sources (path) values (@path)"""

  override def deploy()(implicit connection: Connection): Unit =
    Ignore.ignore(
      """CREATE TABLE sources(
        |  id serial PRIMARY KEY,
        |  path text UNIQUE
        |)
      """.stripMargin
    )

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE sources")
}
