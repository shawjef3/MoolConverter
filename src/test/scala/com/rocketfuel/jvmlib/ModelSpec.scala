package com.rocketfuel.jvmlib

import com.google.common.jimfs.{Configuration, Jimfs}
import com.rocketfuel.mool
import _root_.java.nio.file._
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterEach, FunSuite}

class ModelSpec
  extends FunSuite
  with BeforeAndAfterEach {

  var fs: FileSystem = null

  var root: Path = null

  test("copies") {
    val moolModel = mool.Model.ofRepository(root)
    val models = Models.ofMoolRepository(root)

    val expectedModels =
      Models(
        models =
          Map(
            Vector("java", "0") -> Model(
              identifier = Some(Model.Identifier("test_group", "test_artifact", "0.0")),
              configurations = Map(
                "main" -> Model.Configuration(dependencies = Set.empty, files = Set(root.resolve("java/0.java"))),
                "test" -> Model.Configuration(dependencies = Set(Model.Dependency.Local(Vector("java", "0"))), files = Set(root.resolve("java/0Test.java")))
              )
            )
          ),
        moolModel = moolModel,
        moolRoot = root
      )

    assertResult(expectedModels)(models)

    val copiesRoot = root.resolve("copies")

    val expectedCopies =
      Map(
        root.resolve("java/0.java") -> copiesRoot.resolve("0/src/main/java/0.java"),
        root.resolve("java/0Test.java") -> copiesRoot.resolve("0/src/test/java/0Test.java")
      )

    val copies =
      models.copies(copiesRoot)

    assertResult(expectedCopies)(copies)
  }

  override protected def beforeEach(): Unit = {
    fs = Jimfs.newFileSystem(Configuration.unix())
    root = fs.getPath("/")
    Files.createDirectories(root.resolve("java"))

    for (file <- Vector("java/RELCFG", "java/BLD")) {
      val in = getClass.getResourceAsStream(file)
      val out = Files.newOutputStream(root.resolve(file))
      IOUtils.copy(in, out)
      out.close()
      in.close()
    }
  }

  override protected def afterEach(): Unit = {
    fs.close()
  }
}
