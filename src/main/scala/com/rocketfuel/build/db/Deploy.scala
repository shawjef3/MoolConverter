package com.rocketfuel.build.db

import com.rocketfuel.build.db.mool._
import com.rocketfuel.sdbc.PostgreSql

object Deploy extends Deployable {
  val deploy: Deployable =
    Seq(Bld, BldToDep, Source, BldToSource, RelCfg).fold(Deployable.empty)(_ + _)

  override def deploy()(implicit connection: PostgreSql.Connection): Unit =
    deploy.deploy()

  override def undeploy()(implicit connection: PostgreSql.Connection): Unit =
    deploy.undeploy()
}
