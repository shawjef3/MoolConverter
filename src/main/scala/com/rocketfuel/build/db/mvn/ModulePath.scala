package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.{Deployable, SelectableById}
import com.rocketfuel.sdbc.PostgreSql._

case class ModulePath(
  id: Int,
  path: String
)

object ModulePath extends Deployable with SelectableById[ModulePath] {
  val list = Select[ModulePath]("SELECT * FROM mvn.module_paths")

  def byId()(implicit connection: Connection): Map[Int, ModulePath] = {
    for ((id, paths) <- ModulePath.list.vector().groupBy(_.id)) yield {
      if (paths.size != 1)
        throw new RuntimeException("unexpected duplicate primary id")
      (id, paths.head)
    }
  }

  val deployQuery = Ignore.readClassResource(classOf[Identifier], "module_paths.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.module_paths CASCADE")

  override val selectByIdSql: CompiledStatement =
    "SELECT * FROM mvn.module_paths WHERE id = @id"
}
