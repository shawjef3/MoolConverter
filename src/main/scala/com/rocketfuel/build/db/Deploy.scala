package com.rocketfuel.build.db

import com.rocketfuel.build.db.mool._
import com.rocketfuel.sdbc.PostgreSql

object Deploy extends Deployable {
  val deploy: Deployable =
    Seq(
      Bld,
      BldToBld,
      Source,
      BldToSource,
      RelCfg,
      RelCfgToBld,
      SourceToBld,
      SourceConflicts,
      TestBlds
    ).fold(Deployable.empty)(_ + _)

  override def deploy()(implicit connection: PostgreSql.Connection): Unit =
    deploy.deploy()

  override def undeploy()(implicit connection: PostgreSql.Connection): Unit =
    deploy.undeploy()
}
