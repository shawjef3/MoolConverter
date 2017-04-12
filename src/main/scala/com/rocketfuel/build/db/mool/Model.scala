package com.rocketfuel.build.db.mool

import com.rocketfuel.sdbc.PostgreSql._
import org.postgresql.util.PSQLException

object Model {

  def insert(model: com.rocketfuel.build.mool.Model)(implicit connection: Connection): Unit = {

    /*
    insert BLDs
    insert sources
    insert bld source mappings
    insert bld dependency mappings
    insert bld compile dependency mappings

    insert relcfgs
     */

    val dbBlds =
      for {
        (bldPath, bld) <- model.blds
      } yield bldPath -> Bld.create(bldPath, bld)

    for ((bldPath, bld) <- model.blds) {
      val dbBld = dbBlds(bldPath)

      val sources =
        for {
          src <- bld.srcPaths(model, bldPath)
        } yield Source.insertOrSelect(model.root.relativize(src).toString)

      for (source <- sources)
        try BldToSource.insert(BldToSource(0, dbBld.id, source.id))
        catch {
          case e: PSQLException if e.getMessage.startsWith("ERROR: duplicate key value violates unique constraint") =>
            //The bld already has the source, so don't do anything.
            ()
        }

      for (dep <- bld.depPaths(bldPath)) {
        val dbDep = Bld.selectByPath(dep).get
        try BldToBld.insert(BldToBld(0, dbBld.id, dbDep.id, isCompile = false))
        catch {
          case e: PSQLException if e.getMessage.startsWith("ERROR: duplicate key value violates unique constraint") =>
            ()
        }
      }

      for (dep <- bld.compileDepPaths(bldPath)) {
        val dbDep = Bld.selectByPath(dep).get
        try BldToBld.insert(BldToBld(0, dbBld.id, dbDep.id, isCompile = true))
        catch {
          case e: PSQLException if e.getMessage.startsWith("ERROR: duplicate key value violates unique constraint") =>
            ()
        }
      }
    }

    for ((relCfgPath, relCfg) <- model.relCfgs) {
      val db =
        RelCfg.insert(
          RelCfg(
            id = 0,
            path = relCfgPath.mkString("."),
            groupId = relCfg.group_id,
            artifactId = relCfg.artifact_id,
            baseVersion = relCfg.base_version
          )
        )

      val noDepsBld =
        for (noDeps <- relCfg.`jar-no-dependencies`) yield {
          //drop(5) removes "mool."
          val dbBld = Bld.selectByPath(noDeps.target.drop(5)).get

          RelCfgToBld.insert(RelCfgToBld(0, db.id, dbBld.id, false, noDeps.artifact_path))
        }

      val depsBld =
        for (withDeps <- relCfg.`jar-with-dependencies`) yield {
          //drop(5) removes "mool."
          val dbBld = Bld.selectByPath(withDeps.target.drop(5)).get

          RelCfgToBld.insert(RelCfgToBld(0, db.id, dbBld.id, true, withDeps.artifact_path))
        }
    }

  }
}
