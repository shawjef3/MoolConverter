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

      for {
        dep <- bld.deps.getOrElse(Vector.empty)
        dbDep <- Bld.selectByPath(dep)
      } BldToDep.insert(BldToDep(0, dbBld.id, dbDep.id, isCompile = false))

      for {
        dep <- bld.compileDeps.getOrElse(Vector.empty)
        dbDep <- Bld.selectByPath(dep)
      } BldToDep.insert(BldToDep(0, dbBld.id, dbDep.id, isCompile = true))
    }

    for ((relCfgPath, relCfg) <- model.relCfgs) {
      RelCfg.create(relCfgPath, relCfg)
    }

  }
}
