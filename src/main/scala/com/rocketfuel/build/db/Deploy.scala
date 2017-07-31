package com.rocketfuel.build.db

import com.rocketfuel.sdbc.PostgreSql._

object Deploy extends Deployable {
  val deploy: Deployable =
    mool.Schema +
      mool.Bld +
      mool.BldToBld +
      mool.Source +
      mool.BldToSource +
      mool.RelCfg +
      mool.RelCfgToBld +
      mool.Version +
      mool.SourceToBld +
      mool.SourceConflicts +
      mool.TestBlds +
      mool.Dealias +
      mool.IsAlias +
      mool.dedup.Deploy +
      mvn.Schema +
      gradle.Schema +
      gradle.BldJoins +
      mvn.ModulePath +
      mvn.Copy +
      mvn.Identifier +
      mvn.DependencySupplements +
      mvn.Dependency +
      mvn.Exclusion

  override def deploy()(implicit connection: Connection): Unit =
    deploy.deploy()

  override def undeploy()(implicit connection: Connection): Unit =
    deploy.undeploy()
}
