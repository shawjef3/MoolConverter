package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

case class Source(
  path: String
)

object Source extends Deployable with InsertableToValue[Source] {

  override val insertSql: CompiledStatement =
    """INSERT INTO sources (path) values (@path)"""

  override def deploy()(implicit connection: Connection): Unit =
    Ignore(
      """CREATE TABLE sources(
        |  id serial PRIMARY KEY,
        |  path text UNIQUE
        |)
      """.stripMargin
    ).ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore("DROP TABLE sources").ignore()
}
