package com.rocketfuel.build.db.mool

import com.rocketfuel.build.Logger
import com.rocketfuel.build.mool.{Bld => MoolBld, MoolPath}
import com.rocketfuel.sdbc.PostgreSql._
import org.postgresql.util.PSQLException

class Model(model: com.rocketfuel.build.mool.Model) extends Logger {

  def insert()(implicit connection: Connection): Unit = {

    /*
    insert BLDs
    insert sources
    insert bld source mappings
    insert bld dependency mappings
    insert bld compile dependency mappings

    insert relcfgs
     */

    val dbBlds =
      for ((bldPath, bld) <- model.blds) yield
        bldPath -> Bld.create(bldPath, bld)

    for ((bldPath, bld) <- model.blds) {
      val dbBld = dbBlds(bldPath)

      val sources =
        for {
          src <- bld.srcPaths(model, bldPath)
        } yield {
          val relativePath = model.root.relativize(src).toString
          Source.insertOrSelect(relativePath)
        }

      for (source <- sources)
        try BldToSource.insert(BldToSource(0, dbBld.id, source.id))
        catch {
          case e: PSQLException if e.getMessage.startsWith("ERROR: duplicate key value violates unique constraint") =>
            //The bld already has the source, so don't do anything.
            logger.warn(s"duplicate source dependency $bldPath -> $source")
        }

      for (depPath <- bld.depPaths(bldPath)) {
        val dbDep = dbBlds(depPath)

        try BldToBld.insert(BldToBld(0, dbBld.id, dbDep.id, isCompile = false))
        catch {
          case e: PSQLException if e.getMessage.startsWith("ERROR: duplicate key value violates unique constraint") =>
            logger.warn(s"duplicate bld dependency $bldPath -> $depPath")
        }
      }

      for (depPath <- bld.compileDepPaths(bldPath)) {
        val dbDep = dbBlds(depPath)
        try BldToBld.insert(BldToBld(0, dbBld.id, dbDep.id, isCompile = true))
        catch {
          case e: PSQLException if e.getMessage.startsWith("ERROR: duplicate key value violates unique constraint") =>
            logger.warn(s"duplicate bld dependency $bldPath -> $depPath")
        }
      }
    }

    for ((relCfgPath, relCfg) <- model.relCfgs) {
      val db =
        RelCfg.insert(
          RelCfg(
            id = 0,
            path = relCfgPath,
            groupId = relCfg.group_id,
            artifactId = relCfg.artifact_id,
            baseVersion = relCfg.base_version
          )
        )

      import com.rocketfuel.build.mool.{RelCfg => MRelCfg}
      val noDepsBld =
        for (noDeps <- List(relCfg.`jar-no-dependencies`, relCfg.deploy).flatten.toSet[MRelCfg.Artifact]) yield {
          //drop(5) removes "mool."
          val dbBld = Bld.selectByPath(noDeps.target.drop(5)).get

          RelCfgToBld.insert(RelCfgToBld(0, db.id, dbBld.id, false, noDeps.artifact_path))
        }

//      val depsBld =
//        for (withDeps <- relCfg.`jar-with-dependencies`) yield {
//          //drop(5) removes "mool."
//          val dbBld = Bld.selectByPath(dealias(model, withDeps.target.split('.').toVector.tail)).get
//
//          RelCfgToBld.insert(RelCfgToBld(0, db.id, dbBld.id, true, withDeps.artifact_path))
//        }
    }

    for {
      (path, versions) <- model.versions
      version <- versions
    } {
      Version.insert(Version(0, path, version.artifactId, version.commit, version.version))
    }

  }

}
