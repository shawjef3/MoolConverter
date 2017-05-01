package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

object DependencySupplements extends Deployable {

  override def deploy()(implicit connection: Connection): Unit = {
    Ignore.readTypeResource[DependencySupplements.this.type]("dependency_supplement_create.sql").ignore()
  }

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP TABLE IF EXISTS mvn.dependency_supplements CASCADE")

  def supplement()(implicit connection: Connection): Unit = {
    Ignore.readTypeResource[DependencySupplements.this.type]("dependency_supplement_insert.sql").ignore()
  }

}
