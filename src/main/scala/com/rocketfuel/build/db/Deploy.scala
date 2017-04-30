package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

object Deploy extends Deployable {
  val deploy: Deployable =
    Seq(
      mool.Schema,
      mool.Bld,
      mool.BldToBld,
      mool.Source,
      mool.BldToSource,
      mool.RelCfg,
      mool.RelCfgToBld,
      mool.Version,
      mool.SourceToBld,
      mool.SourceConflicts,
      mool.TestBlds,
      mool.Dealias,
      mool.IsAlias,
      mvn.Schema,
      mvn.ModulePath,
      mvn.Copy,
      mvn.Identifier,
      mvn.DependencySupplements,
      mvn.Dependency
    ).fold(Deployable.empty)(_ + _)

  override def deploy()(implicit connection: Connection): Unit =
    deploy.deploy()

  override def undeploy()(implicit connection: Connection): Unit =
    deploy.undeploy()
}
