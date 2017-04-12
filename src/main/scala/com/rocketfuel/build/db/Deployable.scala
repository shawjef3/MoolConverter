package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql
import com.rocketfuel.sdbc.PostgreSql._

trait Deployable {
  parent =>

  def deploy()(implicit connection: Connection): Unit

  def undeploy()(implicit connection: Connection): Unit

  def +(other: Deployable): Deployable =
    if (this eq other)
      this
    else new Deployable {
      override def deploy()(implicit connection: Connection): Unit = {
        parent.deploy()
        other.deploy()
      }

      override def undeploy()(implicit connection: Connection): Unit = {
        other.undeploy()
        parent.undeploy()
      }
    }

}

object Deployable {
  val empty =
    new Deployable {
      override def deploy()(implicit connection: PostgreSql.Connection): Unit = ()

      override def undeploy()(implicit connection: PostgreSql.Connection): Unit = ()
    }
}
